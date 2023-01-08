/*

MITHNoteMidiMap

used with MITHNoteViewer

Dependencies: MITHUnicode

© 2022 Tom Hall
www.ludions.com

*/


MITHNoteMidiMap {

	var midiRaw, signed, octave, pc, midiAbs, noteClass;
	var accidntlBool, alteration, accidental, glyphs;
	var <accidentalsMap, glyphsDict, position;

	*new {|signed = false|
		^super.new.init(signed);
	}

	init{|argSigned|
		signed = argSigned;
		this.glyphs;
		this.accidentalsMap_;
	}

	mapMIDI { |midi|
		midiRaw = midi;
		midiAbs = midi.abs;
		octave = (midiAbs/12).floor.asInteger;
		pc = midiAbs%12;
		accidntlBool = accidentalsMap.keys.asArray.includes(pc);
		^this.processMidi;

	}

	map {|midi, showNatural = false|

		this.mapMIDI(midi);

		glyphs = this.mapAccidentalsToGlyphs(accidental, showNatural);
		^[position, glyphs, midi, showNatural]
	}

	accidentalsMap_ {|dict|
		accidentalsMap = dict ?? (1: 1, 3: -1, 6: 1, 8: 1, 10: -1);
		^this
	}

	pcToNoteClass {|pc|
		^[0, 2, 4, 5, 7, 9, 11].indexOf(pc)
	}

	flatten {
		alteration = -1;
		accidental = \flat;
		^this;
	}

	// using signed ints, pcs 11 and 4
	// can be flattened but not sharpened
	// (this isn't possible in Max)
	checkForCorFFlats{
		if(midiRaw.isNegative){
			if(pc==11){
				this.flatten;
				octave=octave+1;
			};
			if(pc==4){
				this.flatten;
			};
		};
		^this;
	}

	processMidi {

		if(accidntlBool){
			alteration = if(signed){
				midiRaw.sign.asInteger
			}{
				accidentalsMap.at(pc)
			};
			case
			{alteration == 1}{
				accidental = \sharp;
				// C written as B-sharp loses an octave
				if(pc==0){octave=octave-1};
			}
			{alteration == -1}{
				accidental = \flat;
				// only for unsigned
				if(pc==11){octave=octave+1}; // C-flat
			};
		}{
			alteration = 0;
			accidental = \natural;
		};

		// edge cases needed for signed Fb and Cb
		if(signed){this.checkForCorFFlats};
		noteClass = (pc + alteration.neg)%12;
		// noteClass essentially C-natural scale degree -1 (0 -> 6)
		// or a position class (minus octave)
		noteClass = this.pcToNoteClass(noteClass);
		position = noteClass + (octave * 7);
		^[position, pc, noteClass, accidental, octave];
	}


	mapAccidentalsToGlyphs {|accidental, showNatural|

		case
		{accidental==\natural and: {showNatural}}{^glyphsDict[\naturalNote]}
		{accidental==\natural}{^glyphsDict[\plainNote]} // showNatural false
		{accidental==\sharp}{^glyphsDict[\sharpNote]}
		{accidental==\flat}{^glyphsDict[\flatNote]}
		{true}{^"accidental not recognised".error};
	}

	// https://www.w3.org/2021/03/smufl14/tables/standard-accidentals-12-edo.html
	glyphs {
		var plainNote, sharpNote, flatNote, naturalNote;
		var accidentalSharp, accidentalFlat;
		var accidentalNatural, noteheadBlack;

		accidentalSharp = "U+E262";
		accidentalFlat = "U+E260";
		accidentalNatural = "U+E261";
		noteheadBlack = "U+E0A4";

		plainNote = noteheadBlack.asGlyph.asString;
		sharpNote = [accidentalSharp, " ", noteheadBlack].asUnicodeString;
		flatNote = [accidentalFlat, " ", noteheadBlack].asUnicodeString;
		naturalNote = [accidentalNatural, " ", noteheadBlack].asUnicodeString;
		glyphsDict =Dictionary.new;
		glyphsDict.putPairs([
			\plainNote, plainNote,
			\sharpNote, sharpNote,
			\naturalNote, naturalNote,
			\flatNote, flatNote
		]);
		^this;
	}

}

