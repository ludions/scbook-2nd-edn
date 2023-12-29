/*

MITHNoteMidiMap

used with MITHNoteViewer

Dependencies: MITHUnicode

© 2023 Tom Hall
www.ludions.com

*/


MITHNoteMidiMap {

	var midiRaw, signed, octave, pc, midiAbs, noteClass;
	var accidntlBool, alteration, accidental, glyphs;
	var <accidentalsMap, glyphsDict, vPos, glyphsArr;

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

	// returns array of indivudal glyphs [notehead, accidental]
	// accidental may be empty string;
	map {|midi, showNatural = false|
		var note, acc;
		this.mapMIDI(midi);
		note = glyphsDict[\noteheadBlack]; // black notehead
		acc = this.mapAccidentalsToGlyphs(accidental, showNatural);
		glyphsArr = [note, acc]
		^[vPos, glyphsArr, midi, showNatural]
	}

	mapAccidentalsToGlyphs {|accidental, showNatural|
		var acc;
		acc = case
		{accidental==\natural and: {showNatural}}{glyphsDict[\accidentalNatural]}
		{accidental==\natural}{""} // showNatural false
		{accidental==\sharp}{glyphsDict[\accidentalSharp]}
		{accidental==\flat}{glyphsDict[\accidentalFlat]}
		{true}{^"accidental not recognised".error};
		^acc
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
		// or a vPos class (minus octave)
		noteClass = this.pcToNoteClass(noteClass);
		vPos = noteClass + (octave * 7);
		^[vPos, pc, noteClass, accidental, octave];
	}


	// https://www.w3.org/2021/03/smufl14/tables/standard-accidentals-12-edo.html
	glyphs {
		var accidentalSharp, accidentalFlat;
		var accidentalNatural, noteheadBlack;
		var accidentalDoubleSharp, accidentalDoubleFlat;
		var accidentalNaturalFlat, accidentalNaturalSharp;
		var noteheadWhole, noteheadHalf; // semi-breve, minim

		accidentalSharp = "U+E262";
		accidentalFlat = "U+E260";
		accidentalNatural = "U+E261";
		accidentalDoubleSharp = "U+E263";
		accidentalDoubleFlat = "U+E264";
		accidentalNaturalFlat = "U+E267";
		accidentalNaturalSharp = "U+E268";
		noteheadBlack = "U+E0A4";
		noteheadWhole = "U+E0A2";
		noteheadHalf = "U+E0A3";

		glyphsDict = Dictionary.new;
		glyphsDict.putPairs([
			\accidentalSharp, accidentalSharp.asGlyph.asString,
			\accidentalFlat, accidentalFlat.asGlyph.asString,
			\accidentalNatural, accidentalNatural.asGlyph.asString,
			\accidentalDoubleSharp, accidentalDoubleSharp.asGlyph.asString,
			\accidentalDoubleFlat, accidentalDoubleFlat.asGlyph.asString,
			\accidentalNaturalFlat, accidentalNaturalFlat.asGlyph.asString,
			\accidentalNaturalSharp, accidentalNaturalSharp.asGlyph.asString,
			\noteheadBlack, noteheadBlack.asGlyph.asString,
			\noteheadWhole, noteheadWhole.asGlyph.asString,
			\noteheadHalf, noteheadHalf.asGlyph.asString
		]);
		^this;
	}

}

