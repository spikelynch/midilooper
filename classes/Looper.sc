
// Looper is a class for recording and playing back notes with a simple
// TempoClock-driven event loop.


LooperNote {
	var <label, <index, <rawtime, <>time, <abstime, <>duration, <gated, <synth, <params;

	*new { | label, index, rawtime, abstime, duration, gated, synth, params |
		^super.new.init(label, index, rawtime, abstime, duration, gated, synth, params);
	}

	init { | al, ai, ar, aa, ad, ag, as, ap |
		label = al;
		index = ai;
		rawtime = ar;
		time = ar;
		abstime = aa;
		duration = ad;
		gated = ag;
		synth = as;
		params = ap;
	}

	reset {
		time = rawtime
	}

}


Looper {
	var bpm, beatsperbar, size, tc, <notes, keys, <recording, <playing, <loop;

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
		});
		notes = List.new(0);
		keys = Array.newClear(size);
		recording = nil;
		playing = false;
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

	add { | note |
		^notes.add(note);
	}

	remove { | fn |
		^notes.removeAllSuchThat(fn);
	}

	play {
		playing = true;
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
			if( playing, { beatsperbar }, { nil });
		}, quant: beatsperbar);
	}

	pause {
		playing = false;
	}

	quantize { | q |
		if( q.isNil,
			{ notes.do( { | note | note.reset }); },
			{ notes.do({ | note | note.time = note.rawtime.round(q) }) } );
	}


}



