
// Looper is a class for recording and playing back notes on a loop

// ~notes is a list of notes like:

// [ label, key, bartime, abstime, duration, velocity, [ synth, params ] ]
//
// the sequencer schedules everything in this array at the start of a bar.
//
// bartime is the start of the note in beats relative to the bar
// key is the relative midi note
// abstime is the time in absolute beats when the note was started (for calculating durations)
// duration is the length of the note - it will be nil for a percussive note
// velocity is the loudness
// [ synth, params ] are which synth and how to play it, from the ~insts data structure


LooperNote {
	var <label, <index, <time, <abstime, <>duration, <gated, <synth, <params;

	*new {
		^super.newCopyArgs
	}

}


Looper {
	var bpm, beatsperbar, size, tc, <notes, keys, <recording, <loop;

	*new { | bpm=120, beatsperbar=4, size=88 |
		^super.new.init(bpm, beatsperbar, size);
	}

	init { | argbpm, argbpb, argsize |
		bpm = argbpm;
		beatsperbar = argbpb;
		size = argsize;
		tc = TempoClock.new(bpm / 60, queueSize: 1024);
		tc.schedAbs(tc.nextBar, {
			tc.beatsPerBar_(beatsperbar);
			[ "set beatsperbar to ", beatsperbar ].postln;
		});
		notes = List.new(0);
		keys = Array.newClear(size);
		recording = nil;
	}

	recordingOn { | label |
		recording = label
	}

	recordingOff {
		recording = nil
	}

	startNote { | i, synth, gated, params |
		var beat = tc.beats, bar = tc.bar, time, note = nil;
		if( recording.notNil, {
			time = beat - tc.bars2beats(bar);
			note = LooperNote(recording, i, time, beat, nil, gated, synth, params);
			notes.add(note);
		});
		^note;
	}

	stopNote { | i, note |
		var beat = tc.beats;
		[ "stopNote", i, beat, note.abstime ].postln;
		if( recording.notNil, { note.duration_(beat - note.abstime) });
	}

	playKey { | note |
		if( note.gated && keys[note.index].notNil, { this.releaseKey(note) });
		keys[note.index] = Synth.head(nil, note.synth, note.params);
	}

	releaseKey { | note |
		if( keys[note.index].notNil, {
			keys[note.index].release;
			keys[note.index] = nil;
		})
	}


	play {
		loop = tc.play({
			notes.do({ | note, ni |
				var node;
				tc.sched(note.time, { this.playKey(note) });
				if( note.gated, {
					if(note.duration.notNil, {
						tc.sched(note.time + note.duration, { this.releaseKey(note) });
					},
					{
						// if there's no duration, schedule an event in
						// a bar's time to look for the duration then
						// schedule another release event - this doesn't
						// work quite as well as I would like which is
						// why playKey has to make sure that the key is
						// released
						tc.sched(beatsperbar, {
							if( note.duration.notNil, {
								tc.sched(beatsperbar - note.time + note.duration, {
									this.releaseKey(note);
								})
							},
							{
								[ "warning: note without duration after waiting a bar", note.index ].postln;
								this.releaseKey(note);
							})
						});
					});
				});
			});
			beatsperbar;
		}, quant: beatsperbar);
	}




}

