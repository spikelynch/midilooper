// a class which binds instruments to keys on a midi controller
// and manages playing and releasing the synths

MidiInstrument {
	var <label, <low, <high, <synth, <fn, <gated;

	*new { | label, low, high, synth, fn |
		^super.new.init(label, low, high, synth, fn)
	}

	init { | arglabel, arglow, arghigh, argsynth, argfn |
		var synthdesc;
		label = arglabel;
		low = arglow;
		high = arghigh;
		synth = argsynth;
		fn = argfn;
		synthdesc = SynthDescLib.match(synth);
		if( synthdesc.isNil, {
			Error("No SynthDef called %".format(synth)).throw;
		},
		{
			gated = synthdesc.hasGate;
		});
	}

	play { | m, vel |
		^fn.value(m - low, vel)
	}
}


MidiKeyboard {
	var <low, <high, <size, keys, <instruments, recorder, keyOnFn, keyOffFn;

    *new { | low=21, high=108 |
        ^super.new.init(low, high)
    }

    init { | arglow, arghigh |
		low = arglow;
		high = arghigh;
		size = high - low + 1;
		keys = Array.newClear(size);
		instruments = List.new(0);
		recorder = nil;
		keyOnFn = nil;
		keyOffFn = nil;
	}

	addInstrument { | label, ilow, ihigh, synth, fn |
		if(( ihigh < ilow ), { Error("highest note must be >= lowest note").throw });
		if((ilow < low), { Error("low note out of range").throw });
		if((ihigh > high), { Error("high note out of range").throw });
		// todo - warn for overlaps
		instruments.addFirst(MidiInstrument(label, ilow, ihigh, synth, fn))
		^instruments.first;
	}

	removeInstrument { | label |
		^instruments.removeAllSuchThat({ | inst | inst.label == label});
	}

	getInstrument { | i |
		^instruments.select({ | inst | (i >= inst.low) && (i <= inst.high) })[0]
	}

	// a recorder is something with startNote and endNote methods

	sendNotes { | argrecorder |
		recorder = argrecorder;
	}

	initMIDI {
		MIDIClient.init;
		MIDIIn.connectAll;

		keyOnFn = MIDIFunc.noteOn({ | vel, m |
			var i, inst, note = nil;
			i = m - low;
			inst = this.getInstrument(m);
			if(inst.notNil, {
				var params = inst.play(m, vel);
				if( recorder.notNil, {
					note = recorder.startNote(i, inst.synth, inst.gated, params);
				});
				keys[i] = [ Synth.head(nil, inst.synth, params), note ];
			}, { "No instrument found for midi note %".format(m).postln });
		});

		keyOffFn = MIDIFunc.noteOff({ | vel, m |
			var i, inst;
			i = m - low;
			inst = this.getInstrument(m);
			if(inst.notNil, {
				if(inst.gated, {
					var key = keys[i];
					if(key.notNil, {
						key[0].release;
						if( key[1].notNil && recorder.notNil, {
							recorder.stopNote(i, key[1])
						});
						keys[i] = nil;
					})
				})
			});
		});

	}

	// Doing it in this class so that it can use the instruments
	// i and j are key notes relative to the low note of the controller

	metronome { | label, n, i, j |
		var insti = this.getInstrument(i), instj = this.getInstrument(j), paramsi, paramsj;
		paramsi = insti.play(i, 64);
		paramsj = instj.play(j, 64);
		(0..n-1).do({
			|m|
			recorder.add(LooperNote(label, i - low, m * 2, 0, 0.125, insti.gated, insti.synth, paramsi));
			recorder.add(LooperNote(label, j - low, m * 2 + 1, 0, 0.125, instj.gated, instj.synth, paramsj));
		});
	}







}