// a class which binds instruments to keys on a midi controller
// and manages playing and releasing the synths

MidiInstrument {
	var <label, <low, <high, <synth, <fn;

	*new {
		^super.newCopyArgs
	}

	hasGate {
		^SynthDescLib.match(synth).hasGate;
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
		^instruments;
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
				var params = inst.fn.value(m - inst.low, vel);
				if( recorder.notNil, {
					note = recorder.startNote(i, inst.synth, inst.hasGate, params);
				});
				keys[i] = [ Synth.head(nil, inst.synth, params), note ];
				[ "keyOn", i, keys[i] ].postln;
			}, { "No instrument found for midi note %".format(m).postln });
		});

		keyOffFn = MIDIFunc.noteOff({ | vel, m |
			var i, inst;
			i = m - low;
			inst = this.getInstrument(m);
			if(inst.notNil, {
				if(inst.hasGate, {
					var key = keys[i];
					[ "keyOff", i, key ].postln;
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






}