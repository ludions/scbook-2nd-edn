/*

MITHNoteViewer
nslider-like

Dependencies:
- MITHNoteMidiMap
- MITHUnicode
- ViewCentral
- a SMuFL font, e.g. Bravura (default) https://github.com/steinbergmedia/bravura/

// check if default font Bravura is installed
Font.availableFonts.collect({|i| (i == "Bravura")}).includes(true)

© 2023 Tom Hall
www.ludions.com

*/


MITHNoteViewer {
	var <fontSize, <fontName, <models, <views, <fontBool;
	var <>mappingClassName, <mapper, <signed, <accidentalsMap, clefWidth;
	var <winHeight, <winWidth, <staveSlots, <foreground;
	var <winModel, <winBackground, <background;

	*new {|staveSlots, winHeight, signed = true, fontName, winPos, mappingClassName|
		^super.new.init(staveSlots, winHeight, signed, fontName, winPos, mappingClassName);
	}

	init {|argStaveSlots, argWinHeight, argSigned, argFontName, argWinPos, argMappingClassName|

		var idealWinHeight, winModelMargins, winPosInit;

		fontName = argFontName ?? "Bravura";
		fontBool = Font.availableFonts.collect({|i| (i == fontName)}).includes(true);
		if(fontBool.not){
			"The selected or default Font 'Bravura' is not installed".error;
			"Bravura Font can be downloaded at: https://github.com/steinbergmedia/bravura/".postln;
		};
		staveSlots = argStaveSlots ?? 1;
		idealWinHeight = argWinHeight ?? 400;
		signed = argSigned;
		fontSize = (idealWinHeight / 9.75).floor.asInteger;

		winPosInit = argWinPos ?? [25, 25];
		winHeight = fontSize * 9.75; // same as viewHieght in model
		clefWidth = 0.75; // wrt fontSize

		winWidth = clefWidth + staveSlots.collect{|i|
			if(i==(staveSlots-1)){2}{1.5}
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
		views = staveSlots.collect{|i|
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
		widthScaler = if(staveSlots>1){1.5}{2};
		^staveSlots.collect{|i|
			clefsBool = (i==0);
			viewOffset = clefWidth * i.min(1);
			viewOffset = viewOffset  + (widthScaler * i); // wrt fontSize
			viewWidth = if(i==(staveSlots-1)){2}{1.5}; // last view is wider

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

	// for hacking, allows any glyphs, doesn't use midi note num
	// rather, vertical note slot positions between 0-70
	// e.g. middle C-sharp
	// addCustom(35, 0, ["U+E0A4".asGlyph.asString, "U+E262".asGlyph.asString])

	addCustom {|pos, slot = 0, stringArr, color|
		var infoArr, model;
		slot = slot.min(staveSlots-1);
		model = models[slot];
		this.clear(slot);
		if(pos > 70){^"Highest displayable MIDI note is 121".error};
		if(color.isNil){color = foreground};
		infoArr = [pos, stringArr, 121- pos, \true, color];
		model.displayNote(*infoArr);
		^this
	}

	add {|note, slot = 0, showNat = false, color|
		var infoArr, model, vPos;
		slot = slot.min(staveSlots-1);
		model = models[slot];
		this.clear(slot);
		if(note.abs > 121){^"Highest displayable MIDI note is 121".error};
		infoArr = mapper.map(note, showNat); // [vPos, glyphsArr, midi, showNat]
		// glyphsArr is [note, acc]
		vPos = infoArr[0];
		if(color.isNil){color = foreground};
		infoArr = infoArr ++ color;
		model.displayNote(*infoArr);
		^this
	}

	addAll {|noteArr, showNat = false, color|
		noteArr.do{|i, j|
			if(j<staveSlots){
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
		staveSlots.do{|i| this.clear(i) };
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
	var <clefGlyphs, <noteBoxesL, <noteVOffsetsArr, <clefsBool, numNoteVPos;
	var <penWidthLedger, <currentGlyphsArr, <foreground;
	var <currGlyphsInfo, <ledgerLinesArrNarrowPx, <ledgerLinesArrWidePx;


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
		penWidthLedger = penWidth * 1.5;
		numNoteVPos = 71;
		this.calcNotesPos;
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
	calcNotesPos {
		var stepOffset, leftL, leftR, width, height;
		stepOffset = staveSpace * 0.5;
		leftL = clefsWidth + (staveSpace * 3); // left: LH note Horiz placement
		width = staveSpace * 2;
		height = staveSpace * 4; // height NB if changed will alter vPos
		    // c. this height needed for accidentals
		noteBoxesL = numNoteVPos.collect{|i|
			Rect.new(leftL,i * stepOffset, width, height)
		}.reverse;
		^this;
	}


	displayNote {|vPos=0, stringArr, midi, showNat, color|
		this.displayGlyphs(vPos, stringArr, midi, showNat, color);
		this.displayLedgerLines(vPos);
		^this
	}

	displayGlyphs { |vPos, stringArr, midi, showNat, color|

		if(currentGlyphsArr.includes(vPos).not) {
			currentGlyphsArr = currentGlyphsArr.add(vPos);
			currGlyphsInfo.clear; // remove old
			if(color.isNil){color = foreground};
			currGlyphsInfo.put(\vPos, vPos);
			currGlyphsInfo.put(\midi, midi);
			currGlyphsInfo.put(\stringArr, stringArr);
			currGlyphsInfo.put(\bounds, noteBoxesL[vPos]);
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
		// TODO consider new update method
		// changes GUI without rewriting entire note
		this.changed(\glyphsCol, color);
		^this
	}


	displayLedgerLines {|vPos|
		var currLedgerLns;
		currLedgerLns = this.calcLedgerLineDisplay(vPos);
		this.selectLedgerLines(currLedgerLns);
		^this;
	}

	// maps ledger lines to note positions
	calcLedgerLineDisplay {|offset|
		case {offset == 35}{ ^[7] }
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
		ledgerLinesArr = arr.collect{|i| ledgerLinesArrNarrowPx[i]};
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
		var lHeight, lLnWidthN, lLnWidthW, lLnHOffsets, ledgerLinesArrBase;

		lHeight = staveSpace;
		lLnWidthN = staveSpace*2;
		lLnWidthW = staveSpace*3.15;
		lLnHOffsets =  clefsWidth + (staveSpace * 3.4);

		ledgerLinesArrBase = [0, 1, 2, 3, 4, 10, 11, 17, 23, 24, 30, 31, 32, 33, 34];
		ledgerLinesArrNarrowPx = ledgerLinesArrBase.collect{|i|
			[
				lLnHOffsets@((i*lHeight)+(lHeight*2.5)),
				(lLnHOffsets+lLnWidthN)@((i*lHeight)+ (lHeight*2.5))
			];
		};
		ledgerLinesArrNarrowPx = ledgerLinesArrNarrowPx.reverse;

		ledgerLinesArrWidePx = ledgerLinesArrBase.collect{|i|
			[
				lLnHOffsets@((i*lHeight)+(lHeight*2.5)),
				(lLnHOffsets+lLnWidthW)@((i*lHeight)+ (lHeight*2.5))
			];
		};
		ledgerLinesArrWidePx = ledgerLinesArrWidePx.reverse;
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
	var model, win, <uView, <spec;
	var <ledgerLinesArr, clefBoxes;
	var <staticTextsDict, foreground;
	var accOffset, <staveLinesArr;

	*new { |model, win|
		^super.new.init(model, win);
	}
	init { |argModel, argWin|

		model = argModel;
		model.addDependant(this);

		win = argWin; // can be a View

		spec = model.staveSpace;
		foreground = model.foreground;

		accOffset = spec * 1.4;

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
		var vPos, midi, accTxtBoxBounds, accidental;
		var accEmpty, noteTxtBox, accTxtBox;

		vPos = glyphsDict[\vPos];
		midi= glyphsDict[\midi];
		noteTxtBox = StaticText.new(uView, bounds: glyphsDict[\bounds]);
		noteTxtBox.align = \right;
		noteTxtBox.font = glyphsDict[\font];
		noteTxtBox.string = glyphsDict[\stringArr][0];
		noteTxtBox.stringColor = glyphsDict[\color];
		accidental = glyphsDict[\stringArr][1];
		accEmpty = accidental.isEmpty;

		if(accEmpty.not, {
			accTxtBoxBounds = glyphsDict[\bounds].copy;
			accTxtBoxBounds.left = accTxtBoxBounds.left - accOffset;
			accTxtBox = StaticText.new(uView, bounds: accTxtBoxBounds);
			accTxtBox.align = \right;
			accTxtBox.font = glyphsDict[\font];
			accTxtBox.string = accidental;
			accTxtBox.stringColor = glyphsDict[\color];
		});

		staticTextsDict.put(vPos, [noteTxtBox, accTxtBox]);
		^this;
	}

	clearGlyphs {|offset|
		staticTextsDict.at(offset).do{|i|
			i.remove
		};
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
				staticTextsDict.do{ |i|
					i[0].stringColor = val;
					i[1].stringColor = val
				}
			}
		}
		{what == \foreground} {
			foreground = val;
			uView.refresh;
			clefBoxes.do{|i| i.stringColor = foreground};
			if(staticTextsDict.notEmpty){
				staticTextsDict.do{ |i|
					i[0].stringColor = foreground;
					i[1].stringColor = foreground
				}
			}
		}
		^this
	}
}













