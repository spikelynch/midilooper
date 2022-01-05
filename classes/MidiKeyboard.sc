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

// a note is a label plus everything that a player will need to
// instantiate it into a Synth. Should this be a SequencerNote?
// as it has no midi info...

MidiNote {
	var <label, <key, <time, <abstime, <duration, <synth, <params;

	*new {
		^super.newCopyArgs
	}

}


MidiKeyboard {
	var <low, <high, <size, keys, <instruments, keyOnFn, keyOffFn;

    *new { | low=21, high=108 |
        ^super.new.init(low, high)
    }

    init { | arglow, arghigh |
		low = arglow;
		high = arghigh;
		size = high - low + 1;
		keys = Array.newClear(size);
		instruments = List.new(0);
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

	// goal here is to make the sequencer take care of the timing
	// information: this class just does

	initMIDI {
		MIDIClient.init;
		MIDIIn.connectAll;

		keyOnFn = MIDIFunc.noteOn({ | vel, m |
			var i, inst;
			i = m - low;
			inst = this.getInstrument(m);
			if(inst.notNil, {
				var params = inst.fn.value(m - inst.low, vel);
				params.postln;
				keys[i] = Synth.head(nil, inst.synth, params);

			}, { "No instrument found for midi note %".format(m) });
		});


			// TODO: hook to send this to the Looper

			//abstime = if(~release.value(i), { beat }, { nil });
			//note = ~addseqnote.value(~recording, i, vel, time, abstime, nil);
			//if(~recording != '', {
			//	n = ~notes.size - 1;
			//} );
			// play the note and store the Synth (if there is one) in keys so it can be released
			//~keys[i] = Synth.head(nil, note[\synth], note[\args]);


			//[n, inote[0][2].value(i, inote[1]) ]; // this is nasty

		keyOffFn = MIDIFunc.noteOff({ | vel, m |
			var i, inst;
			i = m - low;
			inst = this.getInstrument(m);
			if(inst.notNil, {
				if(inst.hasGate, {
					var key = keys[i];
					if(key != nil, {
						key.release;
					})
				})
			});
		});

	}
		//
		//
		// if( ~release.value(i), {
		// 	key = ~keys[i];
		// 	if( key != nil, {
		// 		if( key[0] != nil, {
		// 			note = ~notes[key[0]];
		// 			~notes[key[0]][\duration] = beat - note[\abstime];
		// 		});
		// 		key[1].release;
		// 		~keys[i] = nil;
		// 	});
		// });
		//
		// });
		//




}