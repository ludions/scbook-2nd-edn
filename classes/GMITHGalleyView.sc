/*
© 2022 Tom Hall
*/



// For making GalleyViews
GMITHGalleyViewWindow {
	var <win, <model, <userView, drawFunc, drawingModel;
	var gridLnCol, gridLnWdth, gridPatterns, <>verbose;
	var backgroundCol;

	*new {arg model, win, verbose = false;
		^super.new.init(model, win, verbose);
	}

	init {|argModel, argWin, argVerbose|

		model = argModel;
		model.addDependant(this);
		win = if(argWin.isNil){this.makeWindow}{argWin};
		verbose = argVerbose;
		gridLnWdth = model.gridLnWdth;
		gridPatterns = model.gridPatterns;
		gridLnCol = model.gridLnCol;

		win.bounds_(model.winBounds).front;
		userView = UserView(win);
		userView.bounds_(model.userViewBounds);

		backgroundCol = model.backgroundCol;
		this.setBackgroundCol(backgroundCol);

		win.front;
		win.onClose_({ model.removeDependant(this)});
	}

	makeWindow { arg bounds;
		^Window.new("", bounds: bounds).front;
	}

	setBackgroundCol { |color|
		win.background_(color);
		userView.background_(color);
		^this;
	}

	setDrawFunc {
		// win.background_(Color.rand); // TESTING TMP
		userView.drawFunc = {
			drawingModel.drawGrid(
				width: gridLnWdth,
				color: gridLnCol,
				patterns: gridPatterns,
				dashPattern: model.dashPattern
			);
			if(model.drawEventsBool){
				if(verbose) {
					format("Selected Events to draw are: %", model.selectedEvents).postln;
				};
				drawingModel.drawScoreEvents(
					cells: model.selectedEvents,
					matrix: model.matrix,
					penWidth: model.shapeLnWidth,
					cellRangeOffset: model.eventsRange[0];
				);
			}
		};
		^this;
	}

	// this is the only controller part of this class
	// sends mouse coordinates back to the model
	setMouseFunc {
		userView.mouseDownAction_{|me, x, y, mod|
			var xCell, yCell;
			model.calcMousePressCell(x, y, mod);
		};
	}

	update {|obj, what, val|
		var keys, changeBool;

		if(verbose) {
			format("GMITHGalleyViewModel has changed: %: %", what, val).inform;
		};

		case
		{ what == \winBounds}{
			win.bounds_(val);
		}
		{ what == \userViewBounds}{
			userView.bounds_(val)
		}
		{ what == \gridLnCol}{
			gridLnCol = val;
		}
		{ what == \gridLnWdth}{
			gridLnWdth = val;
		}
		{ what == \gridPatterns}{
			gridPatterns = val;
		}
		{ what == \backgroundCol}{
			backgroundCol = val;
			this.setBackgroundCol(backgroundCol);
		}
		{ what == \drawing}{
			drawingModel = val;
			this.setDrawFunc;
			this.setMouseFunc;
		}
		{ what == \trigViewRefresh}{
			userView.refresh;
		}
		;
		^this;
	}
}

// To be simplifed and made a subclass!?
GMITHGalleyViewModel {
	var screenBounds, <colRange, <cellData, scoreModel, <maxHeightPct;
	var maxHeightPx, <winBounds, vOffset, userView, <gridPatterns, <drawFunc;
	var <dashPattern, <gridLnCol, <gridLnWdth, <curRows, curCols, gridDrawn=false;
	var <sqSize, <scoreSelectModel, <>verbose = false, <eventsRange, <mouseSelect;
	var <selectedEvents, drawing, gridModel, <matrix, <drawEventsBool;
	var <fontSizeScale, <shapeLnWidth, insetGrid, <userViewBounds, <backgroundCol;

	*new {arg scoreSelectModel, maxHeightPct=0.2, vOffset = 25, insetGrid = 6;
		^super.new.init(scoreSelectModel, maxHeightPct, vOffset, insetGrid);
	}

	init {|argScoreSelectModel, argMaxHeightPct, argVOffset, argInsetGrid|

		scoreSelectModel = argScoreSelectModel;
		scoreSelectModel.addDependant(this);
		drawEventsBool = false; // REMOVE HERE AS ALSO BELOW?
		maxHeightPct = argMaxHeightPct;
		vOffset = argVOffset; // window position height offset
		insetGrid = argInsetGrid; // separate to line width offset
		curRows = 3; // default
		screenBounds = Window.availableBounds;
		maxHeightPx = ((screenBounds.height * maxHeightPct).round);
		// only display the title bar until  grid is drawn
		winBounds = Rect(
			screenBounds.left,
			screenBounds.height-vOffset,
			screenBounds.width,
			1
		);
		this.gridPatterns_([1], [1]);
		this.gridLnWdth_(1);
		this.shapeLnWidth_(3);
		this.gridLnCol_(Color.black);
		backgroundCol = Color.white;
		drawEventsBool = false;
		fontSizeScale = 1.0;
		eventsRange = scoreSelectModel.colRange;
		selectedEvents = scoreSelectModel.cellData;
	}

	backgroundCol_ {|color|
		backgroundCol = color;
		this.changed(\backgroundCol, backgroundCol);
	}

	maxHeightPct_ {arg float=0.2;
		maxHeightPct = float.min(1.0); // <=1.0
		this.changed(\maxHeightPct, maxHeightPct);
		if(gridDrawn){this.drawGrid(curRows, curCols)};
		^this;
	}

	fontSizeScale_ {|float|
		fontSizeScale = float;
		if(gridDrawn){
			drawing.fontSize_((sqSize/2.5) * fontSizeScale);
			this.changed(\trigViewRefresh);
		};
	}

	gridPatterns_ {arg x, y;
		gridPatterns = [x, y];
		this.changed(\gridPatterns, gridPatterns);
		if(gridDrawn){
			this.changed(\trigViewRefresh);
		};
		^this;
	}

	gridLnWdth_ {|lineWdth|
		gridLnWdth = lineWdth;
		this.changed(\gridLnWdth, gridLnWdth);
		if(gridDrawn){this.drawGrid(curRows, curCols)};
		^this;
	}

	shapeLnWidth_ {|lineWdth|
		shapeLnWidth = lineWdth;
		this.changed(\shapeLnWidth, shapeLnWidth);
		if(gridDrawn){
			this.changed(\trigViewRefresh);
		};
	}

	gridLnCol_ {arg colour;
		gridLnCol = colour;
		this.changed(\gridLnCol, gridLnCol);
		if(gridDrawn){
			this.changed(\trigViewRefresh);
		};
		^this;
	}

	drawEvents_ { arg bool;
		drawEventsBool = bool;
		this.changed(\trigViewRefresh);
		^this;
	}

	drawEvents {
		^drawEventsBool
	}

	drawGrid { arg rows, cols = 20, gridLineWdth;
		var maxSqSize, newWinBounds, maxHeightPxTmp;
		// curRows = rows ?? 3;
		if(rows.notNil){
			curRows = rows
		};
		// don't try and draw fewer cols than are currently in selection model
		curCols = cols.max(scoreSelectModel.numCols);
		gridDrawn = true;

		if(gridLineWdth.notNil){
			this.gridLnWdth_(gridLineWdth);
		};
		//if(maxHeightPct<=1.0){
		maxHeightPxTmp = maxHeightPct.reciprocal;
		//}; // if height is in pixels, leave as is

		maxSqSize = (screenBounds.height/(curRows*maxHeightPxTmp)).round;
		sqSize = (winBounds.width - (insetGrid * 2) - gridLnWdth -(curCols * 0.01))/curCols;
		sqSize = sqSize.trunc(0.01).min(maxSqSize);
		this.changed(\sqSize, sqSize);

		// number of dashes changes acc. to Pen.width, hence:
		dashPattern = FloatArray[
			(sqSize/14) * gridLnWdth.reciprocal,
			(sqSize/18) * gridLnWdth.reciprocal
		];

		// needed so orig Rect available to ref screenbounds
		newWinBounds = winBounds.copy;
		newWinBounds.width_((sqSize * curCols).ceil + (insetGrid*2) + gridLnWdth);
		newWinBounds.height_((sqSize * curRows).ceil + (insetGrid*2) + gridLnWdth) +1;
		this.changed(\winBounds, newWinBounds);

		userViewBounds = Rect.new(
			insetGrid,
			insetGrid,
			newWinBounds.width - (insetGrid*2),
			newWinBounds.height - (insetGrid*2) + 1
		);
		this.changed(\userViewBounds, userViewBounds);

		gridModel = GMITHGridModel.new(
			size: sqSize,
			rows: curRows,
			cols: curCols,
			margin: gridLnWdth
		);
		// gridModel.setPatterns(*gridPatterns); // this is circular ATM, FIX
		matrix = gridModel.matrix;

		drawing = GMITHDrawing.new(gridModel.lines);
		drawing.fontSize_((sqSize/2.5) * fontSizeScale);
		this.changed(\drawing, drawing); // this triggers a redraw

		^this;
	}



	calcMousePressCell {|x, y, mod|
		var xCell, yCell, assoc, curScore, eventSelect, colOffset = 0, modData;
		// calculate mouse pos in relation to matrix cells
		// [x, y, gridLnWdth, sqSize, curCols, curRows].postln;

		modData = case {mod.isShift}{\shift}
		{mod.isAlt}{\alt}
		{mod.isCtrl}{\ctrl}
		{mod.isFun}{\fun}
		{true}{nil};

		xCell = (((x+(gridLnWdth/2))/sqSize).floor.clip(0, curCols-1)).asInteger;
		yCell = ((y+(gridLnWdth/2))/sqSize).floor.clip(0, curRows-1).asInteger;

		if(eventsRange.isEmpty){
			format("Cell: [ %, % ]", yCell, xCell ).postln;
		}{
			colOffset = eventsRange[0];
			xCell = xCell + colOffset;
			if(xCell<=eventsRange[1]){
				mouseSelect = [yCell, xCell];
				// change model here:
				scoreSelectModel.mouseSelect_(mouseSelect, modData);
				curScore = scoreSelectModel.scoreModel;

				// cell is not empty
				if(curScore.isEmptyCell([ yCell, xCell]).not){
					assoc = curScore.association(
						[ yCell, xCell ]
					);
					eventSelect = assoc;
					scoreSelectModel.eventSelect_(eventSelect);
					//["event detected", eventSelect].postln;
					assoc = assoc.asCompileString;
					// post individual cell if part of multicell event
					if(curScore.isMultiCellEvent([yCell, xCell])){
						format("Cell: [ %, % ]", yCell, xCell ).postln;
					};
					format("Event: %", assoc).postln;
				}{
					// empty cell
					format("Cell: [ %, % ]", yCell, xCell ).postln;
				}
			}{
				format(
					"Cell: [ %, % ] is outside of selected score range", yCell, xCell
				).postln;
			};
		};
		^this
	}

	// score selection has been updated
	update {|obj, what, val|
		var keys, changeBool, newNumCols;

		if(verbose) {
			format("% has noticed score selection has changed: %: %", obj, what, val).inform;
		};
		selectedEvents = scoreSelectModel.cellData;
		eventsRange = scoreSelectModel.colRange;
		newNumCols = scoreSelectModel.numCols;

		if(drawEventsBool and:{gridDrawn}){
			//if the score selection has changed num cols, adjust drawgrid to match as needed
			// if(newNumCols >curCols){
			if(newNumCols != curCols){
				this.drawGrid(curRows, newNumCols);
			}{
				this.changed(\drawing, drawing); // this triggers recalc redraw data
				this.changed(\trigViewRefresh);
			}
		}
	}
}
