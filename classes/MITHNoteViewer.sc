/*

MITHNoteViewer
nslider-like

Dependencies:
- MITHNoteMidiMap
- MITHUnicode
- ViewCentral
- a SMuFL font, e.g. Bravura (default) https://github.com/steinbergmedia/bravura/

// check Font installed
Font.availableFonts.collect({|i| i.compare("Bravura")}).includes(0).not


© 2022 Tom Hall
www.ludions.com

*/


MITHNoteViewer {
	var <fontSize, <fontName, <models, <views, <fontBool;
	var <>mappingClassName, <mapper, <signed, <accidentalsMap, clefWidth;
	var <winHeight, <winWidth, <noteSlots, <foreground;
	var <winModel, <winBackground, <background;

	*new {|noteSlots, winHeight, signed = true, fontName, winPos, mappingClassName|
		^super.new.init(noteSlots, winHeight, signed, fontName, winPos, mappingClassName);
	}

	init {|argNoteSlots, argWinHeight, argSigned, argFontName, argWinPos, argMappingClassName|

		var idealWinHeight, winModelMargins, winPosInit;

		fontName = argFontName ?? "Bravura";
		fontBool = Font.availableFonts.collect({|i| i.compare(fontName)}).includes(0);
		if(fontBool.not){
			"The selected or default Font 'Bravura' is not installed".error;
			"Bravura Font can be downloaded at: https://github.com/steinbergmedia/bravura/".postln;
			"Without a music font installed, only the 'addCustom' method can be used".postln;
		};
		noteSlots = argNoteSlots ?? 1;
		idealWinHeight = argWinHeight ?? 400;
		signed = argSigned;
		fontSize = (idealWinHeight / 9.75).floor.asInteger;

		winPosInit = argWinPos ?? [25, 25];
		winHeight = fontSize * 9.75; // same as viewHieght in model
		clefWidth = 0.75; // wrt fontSize

		winWidth = clefWidth + noteSlots.collect{|i|
			if(i==(noteSlots-1)){2}{1.5}
		}.sum;
		winWidth = winWidth * fontSize; // pixels

		winModelMargins =[0.275 * fontSize, (idealWinHeight-(winHeight))/2];
		winModel = ViewCentralModel.new(
			[winWidth + (0.275 * fontSize * 2), idealWinHeight], // dims
			winModelMargins
		);
		winModel.winPos_(winPosInit);

		foreground = Color.black;
		background = Color.grey(0.995);

		winModel.marginCol_(background);
		winModel.viewCol_(background);
		this.winBackground_(Color.grey(0.95));

		mappingClassName = argMappingClassName ?? "MITHNoteMidiMap";
		mapper = mappingClassName.interpret.new(signed);
		accidentalsMap = mapper.accidentalsMap;

		models = this.makeModels;
		models.do{|i| i.foreground = foreground};
		this.gui;
		^this
	}

	winPos {
		^winModel.winPos
	}

	winPos_ {|x, y|
		winModel.tryPerform(\moveWin, x, y);
		^this
	}

	gui {
		var mainView;
		mainView = ViewCentral.new(Window.new.front, winModel, false).view;
		views = noteSlots.collect{|i|
			MITHNoteViewerGui(models[i], mainView)
		};
		^this;
	}

	foreground_{|color|
		foreground = color;
		models.do{|i| i.foreground = foreground};
		^this
	}

	background_{|color|
		background = color;
		winModel.marginCol_(background);
		winModel.viewCol_(background);
		this.changed(\background, background);
		^this
	}

	winBackground_ {|color|
		winBackground = color;
		winModel.winCol_(winBackground);
		^this
	}

	fullScreen {
		winModel.fullScreen
		^this;
	}

	endFullScreen {
		winModel.endFullScreen
		^this;
	}

	winDims {^[winWidth, winHeight] }

	makeModels {
		var viewOffset, viewWidth, clefsBool, widthScaler;
		widthScaler = if(noteSlots>1){1.5}{2};
		^noteSlots.collect{|i|
			clefsBool = (i==0);
			viewOffset = clefWidth * i.min(1);
			viewOffset = viewOffset  + (widthScaler * i); // wrt fontSize
			viewWidth = if(i==(noteSlots-1)){2}{1.5}; // last view is wider

			MITHNoteViewerModel.new(
				fontSize,
				fontName,
				hOffset: viewOffset,
				clefsBool: clefsBool,
				viewWidth: viewWidth, // clefsWidth added in model as needed
				fontBool: fontBool // is font installed?
			);
		};
	}

	// use when mapping has been changed
	rewriteNotes {
		var currNoteInfo;
		currNoteInfo = [
			models.collect{|i| i.currGlyphsInfo[\midi]},
			models.collect{|i| i.currGlyphsInfo[\showNat]}
		].flop;
		this.clearAll;
		currNoteInfo.do{|i, j|
			if(i[0].notNil){
				this.add(i[0], j, i[1]);
			};
		};
		^this;
	}

	glyphsCol {|slot=0|
		^models[slot].glyphsCol
	}


	glyphsCol_ {|color, slot=0|
		// check slot is not empty
		if(models[slot].glyphsCol.notNil){
			models[slot].glyphsCol_(color);
		}{
			format("No note at slot % to color", slot).error;
		};
		^this
	}

	// for hacking, allows any glyphs
	addCustom {|note, slot = 0, string="x", color|
		var infoArr, ledgerLines, model;
		slot = slot.min(noteSlots-1);
		model = models[slot];
		this.clear(slot);
		if(note.abs > 121){^"Highest displayable MIDI note is 121".error};
		// uses default mapper, ignores glyph returned
		infoArr = mapper.map(note, true); // -> [position, glyphs, midi, showNat]
		infoArr[1] = string;
		if(color.isNil){color = foreground};
		infoArr = infoArr ++ color;
		model.displayGlyphs(*infoArr);
		ledgerLines = model.calcLedgerLineDisplay(infoArr[0]);
		model.selectLedgerLines(ledgerLines);
		^this
	}


	// TODO consider change showNat to showAcc, have nil as default
	// let mapper determine (e.g. naturals false)
	add {|note, slot = 0, showNat = false, color|
		var infoArr, ledgerLines, model;
		slot = slot.min(noteSlots-1);
		model = models[slot];
		this.clear(slot);
		if(note.abs > 121){^"Highest displayable MIDI note is 121".error};
		infoArr = mapper.map(note, showNat); // [position, glyphs, midi, showNat]
		if(color.isNil){color = foreground};
		infoArr = infoArr ++ color;
		model.displayGlyphs(*infoArr);
		ledgerLines = model.calcLedgerLineDisplay(infoArr[0]);
		model.selectLedgerLines(ledgerLines);
		^this
	}

	addAll {|noteArr, showNat = false, color|
		noteArr.do{|i, j|
			if(j<noteSlots){
				this.add(i, j, showNat, color)
			};
		}
	}

	remove {|note, slot=0|
		var pos, model;
		model = models[slot];
		pos = mapper.mapMIDI(note)[0];
		model.removeGlyphs(pos);
		model.selectLedgerLines([]); // remove all lines
		^this;
	}

	clearAll {
		noteSlots.do{|i| this.clear(i) };
		^this
	}


	clear {|slot=0|
		var model, currentGlyphs;
		model = models[slot];
		currentGlyphs = model.currentGlyphsArr.copy;
		currentGlyphs.do{|i|
			model.removeGlyphs(i);
		};
		model.currGlyphsInfo.clear;
		model.selectLedgerLines([]);
		^this
	}

	shrinkWin {
		winModel.shrinkWin;
		^this;
	}

	resizeWin {|float = 1.1|
		winModel.resizeWin(*(winModel.dims * float))
		^this;
	}

	// Max nslider mode; negative integers = flat accidental where possible
	signed_{|bool|
		mapper = mappingClassName.interpret.new(bool);
		mapper.accidentalsMap(accidentalsMap);
		signed = bool;
		^this
	}

	accidentalsMap_{ |dict|
		mapper.accidentalsMap_(dict);
		accidentalsMap = dict;
		this.rewriteNotes;
		^this
	}

	close {
		winModel.close;
		this;
	}

}


MITHNoteViewerModel {
	var <fontName, <font, <fontSize, <hOffset, viewWidth, <fontBool;
	var <uViewBounds, <staveSpace, <staveLinesArr, viewHeight, <penWidth;
	var <ledgerLinesArr, <mCVOffset, <clefInfo, <clefVOffsets, <clefsWidth;
	var <clefGlyphs, <noteBoxes, <noteVOffsetsArr, <clefsBool, numNoteVPos;
	var <ledgerLinesArrBase, <penWidthLedger, <currentGlyphsArr, <foreground;
	var <currGlyphsInfo;

	*new { |fontSize, fontName, clefsBool, hOffset, viewWidth, fontBool|
		^super.new.init(fontSize, fontName, clefsBool, hOffset, viewWidth, fontBool);
	}
	init {|argFontSize, argFontName, argClefsBool, argHOffset, argViewWidth, argFontBool|
		fontBool = argFontBool;
		fontSize = argFontSize ?? 50;
		staveSpace = fontSize/4;
		fontName = argFontName;
		font = Font(fontName, fontSize);
		hOffset = argHOffset ?? 0.375;

		foreground = Color.black;

		clefsBool = argClefsBool ?? true;
		clefsWidth = if(clefsBool){fontSize * 0.75}{0};
		viewHeight = (fontSize * 9) + (staveSpace * 3);
		viewWidth = argViewWidth ?? 2;

		uViewBounds = Rect(
			fontSize * hOffset, // left
			0,
			(fontSize * viewWidth) + clefsWidth,
			viewHeight
		);
		this.clefs(staveSpace*0.4, clefsWidth);
		this.calcStaveLines(viewWidth, clefsWidth);
		this.calcLedgerLines;
		this.selectLedgerLines([], update: false); // initially none
		penWidth = staveSpace*0.09;
		penWidthLedger = penWidth * 1.75;
		numNoteVPos = 71;
		this.notes;
		currentGlyphsArr = [];
		currGlyphsInfo = Dictionary.new;
		^this
	}

	foreground_{|color|
		foreground = color;
		this.changed(\foreground, foreground);
		^this;
	}

	// positions for note StaticText boxes
	notes {
		var stepOffset; //, staveH;
		stepOffset = staveSpace*0.5;
		//staveH = staveSpace;
		noteBoxes = numNoteVPos.collect{|i|
			Rect.new(
				clefsWidth + (staveSpace * 1.5), // note Horiz placement
				(i * stepOffset),
				staveSpace*3,
				staveSpace*4
			)
		}.reverse;
		^this;
	}

	displayGlyphs { |offset=0, string, midi, showNat, color, align = \right|
		var bounds;

		if(currentGlyphsArr.includes(offset).not) {
			currentGlyphsArr = currentGlyphsArr.add(offset);

			currGlyphsInfo.clear; // remove old
			bounds = noteBoxes[offset];
			if(color.isNil){color = foreground}; // NEW 2022-03-21
			currGlyphsInfo.put(\offset, offset); //  debugging (range 0 -> 70)
			currGlyphsInfo.put(\midi, midi); // debugging
			currGlyphsInfo.put(\string, string);
			currGlyphsInfo.put(\align, align);
			currGlyphsInfo.put(\bounds, bounds);
			currGlyphsInfo.put(\font, font);
			currGlyphsInfo.put(\showNat, showNat);
			currGlyphsInfo.put(\color, color);

			this.changed(\glyphsInfo, currGlyphsInfo);
		};
		^this;
	}


	glyphsCol {
		^currGlyphsInfo[\color]
	}

	glyphsCol_ {|color|
		currGlyphsInfo.put(\color, color); // updates here
		// need new update method 2022-03-21
		// changes GUI without rewriting entire note
		this.changed(\glyphsCol, color);
		^this
	}



	// maps ledger lines to note positions
	calcLedgerLineDisplay {|offset|
		var lines, currLedgerLns;
		// currLedgerLns = ledgerLinesArr.collect{|i| ledgerLinesArrBase.indexOf(i)};

		lines = case {offset == 35}{ ^[7] }
		{offset == 47 or:{offset ==48}}{ ^[8] }
		{offset >= 49 and:{offset <61}}{ ^[8, 9] }
		{offset == 61 or:{offset ==62}}{ ^[8, 9, 10] }
		{offset == 63 or:{offset ==64}}{ ^[8, 9, 10, 11] }
		{offset == 65 or:{offset ==66}}{ ^[8, 9, 10, 11, 12] }
		{offset == 67 or:{offset ==68}}{ ^[8, 9, 10, 11, 12, 13] }
		{offset == 69 or:{offset ==70}}{ ^[8, 9, 10, 11, 12, 13, 14] }
		{offset == 23 or:{offset ==22}}{ ^[6] }
		{offset <= 21 and:{offset >9}}{ ^[6, 5] }
		{offset == 9 or:{offset ==8}}{ ^[6, 5, 4] }
		{offset == 7 or:{offset ==6}}{ ^[6, 5, 4, 3] }
		{offset == 5 or:{offset ==4}}{ ^[6, 5, 4, 3, 2] }
		{offset == 3 or:{offset ==2}}{ ^[6, 5, 4, 3, 2, 1] }
		{offset == 1 or:{offset ==0}}{ ^[6, 5, 4, 3, 2, 1, 0] }
		{true}{ ^[] };
	}

	// manages displaying an array of ledger lines (0 -> 14)
	selectLedgerLines {|arr, update= true|
		ledgerLinesArr = arr.collect{|i| ledgerLinesArrBase[i]};
		if(update){
			this.changed(\ledgerLinesArr, ledgerLinesArr);
		};
		^this;
	}


	removeGlyphs {|offset|
		if(currentGlyphsArr.includes(offset)) {
			currentGlyphsArr.remove(offset);
			this.changed(\currentGlyphsArr, offset);
		};
		^this
	}

	clefs { |hOffset, clefsWidth|
		var rects, clefsHeight;

		if(clefsBool) {

			clefInfo = Dictionary.new;
			clefVOffsets = [
				(fontSize*3) + (staveSpace*0.5),
				(fontSize*4) + (staveSpace*0.5)
			];
			clefGlyphs = ["U+E050".asGlyph, "U+E062".asGlyph];

			clefsHeight = fontSize*2.5; // clefs needs this much room

			rects = clefVOffsets.collect{|i|
				Rect(
					hOffset,
					i,
					clefsWidth,
					clefsHeight
				)
			};
			clefInfo.put(\clefsWidth, clefsWidth);
			clefInfo.put(\rects, rects);
			clefInfo.put(\glyphs, clefGlyphs);
			// clefInfo.put(\color, foreground);
		}
		^this;
	}

	calcLedgerLines {
		var lHeight, lLnWidth, lLnHOffsets;

		lHeight = staveSpace;
		lLnWidth = staveSpace*2;
		lLnHOffsets =  clefsWidth + (staveSpace * 2.95);

		ledgerLinesArrBase = [0, 1, 2, 3, 4, 10, 11, 17, 23, 24, 30, 31, 32, 33, 34];
		ledgerLinesArrBase = ledgerLinesArrBase.collect{|i|
			[
				lLnHOffsets@((i*lHeight)+(lHeight*2.5)),
				(lLnHOffsets+lLnWidth)@((i*lHeight)+ (lHeight*2.5))
			];
		};
		ledgerLinesArrBase = ledgerLinesArrBase.reverse;
		^this;
	}


	singleStaveLines {|lnHeight, vOffset=0, viewWidth, clefsWidth|
		var staffLines;
		viewWidth = (viewWidth * fontSize) + clefsWidth + 0.3; // add to avoid gaps
		staffLines= 5.collect{|i|
			i = (i * lnHeight) + vOffset;
			[0@i, viewWidth@i];
		};
		^staffLines
	}


	calcStaveLines {|viewWidth, clefsWidth|
		var staveVOffsets, vOffset;
		vOffset = fontSize +(staveSpace*3.5);
		staveVOffsets = [
			vOffset,
			vOffset + fontSize + (staveSpace*3),
			vOffset + (fontSize * 3) + staveSpace,
			vOffset + (fontSize * 5)
		];
		staveLinesArr = staveVOffsets.collect{|i|
			this.singleStaveLines(staveSpace, i, viewWidth, clefsWidth);
		};
		staveLinesArr = staveLinesArr.flatten(numLevels: 1);
		^this
	}
}



MITHNoteViewerGui {
	var model, win, <uView, <spec, <staveLinesArr;
	var <ledgerLinesArr, clefBoxes, <>noteTxtBox;
	var <staticTextsDict, foreground;

	*new { |model, win|
		^super.new.init(model, win);
	}
	init { |argModel, argWin|

		model = argModel;
		model.addDependant(this);

		win = argWin; // can be a View

		spec = model.staveSpace;
		foreground = model.foreground;

		uView=UserView(win, model.uViewBounds);

		this.makeStaves;
		// check if first slot and font to make clef exists
		if(model.clefsBool and:{model.fontBool}) {
			this.makeClefs;
		};
		staticTextsDict = Dictionary.new;

		// in case gui rebuild and already glyphs
		// previoulsy displayed
		if(model.currGlyphsInfo.notEmpty){
			this.makeGlyphs(model.currGlyphsInfo);
		};

		^this
	}

	makeClefs {
		clefBoxes = Array.fill(2, {StaticText.new(uView)});
		clefBoxes.do{|i, j|
			i.bounds = model.clefInfo[\rects][j];
			i.font = model.font;
			i.string = model.clefInfo[\glyphs][j];
			i.stringColor = foreground;
		};
		^this;
	}

	makeGlyphs {|glyphsDict|
		var offset, midi;

		offset = glyphsDict[\offset]; // debugging;
		midi= glyphsDict[\midi]; // debugging;

		noteTxtBox = StaticText.new(uView, bounds: glyphsDict[\bounds]);
		noteTxtBox.align = glyphsDict[\align];
		noteTxtBox.font = glyphsDict[\font];
		noteTxtBox.string = glyphsDict[\string];
		noteTxtBox.stringColor = glyphsDict[\color]; // changed 2022-03-21

		staticTextsDict.put(offset, noteTxtBox);
		// noteTxtBox.background_(Color.yellow(alpha: 0.4));// testing
		^this;
	}

	clearGlyphs {|offset|
		staticTextsDict.at(offset).remove
		^this;
	}

	// ledger lines also here
	makeStaves {

		staveLinesArr = model.staveLinesArr;
		ledgerLinesArr = model.ledgerLinesArr;

		uView.drawFunc={|uview|
			Pen.width_(model.penWidth);
			Pen.strokeColor_(foreground);
			staveLinesArr.do{|i| Pen.line(i[0], i[1])};
			Pen.stroke;
			if(ledgerLinesArr.notEmpty){
				Pen.width_(model.penWidthLedger);
				ledgerLinesArr.do{|i| Pen.line(i[0], i[1])};
				Pen.stroke;
				Pen.width_(model.penWidth);
			};
		};
	}

	update {|obj, what, val|
		case
		{what == \ledgerLinesArr} {
			ledgerLinesArr = val;
			uView.refresh;
		}
		{what == \glyphsInfo} {
			this.makeGlyphs(val);
		}
		{what == \currentGlyphsArr} {
			this.clearGlyphs(val);
		}
		{what == \glyphsCol} {
			if(staticTextsDict.notEmpty){
				noteTxtBox.stringColor = val
			}
		}
		{what == \foreground} {
			foreground = val;
			uView.refresh;
			clefBoxes.do{|i| i.stringColor = foreground};
			if(staticTextsDict.notEmpty){
				noteTxtBox.stringColor = foreground;
			}
		}
		^this
	}
}


















