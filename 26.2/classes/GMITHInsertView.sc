/*
© 2022 Tom Hall
*/


// For making views to be insterted into a Notation View
GMITHInsertView {
	var model, <userView, drawFunc, drawingModel;
	var gridLnCol, gridLnWdth, gridPatterns, <>verbose;
	var backgroundCol;

	*new {arg model, userView, verbose = false;
		^super.new.init(model, userView, verbose);
	}

	init {|argModel, argUserView, argVerbose|

		model = argModel;
		model.addDependant(this);
		verbose = argVerbose;
		gridLnWdth = model.gridLnWdth;
		gridPatterns = model.gridPatterns;
		gridLnCol = model.gridLnCol;

		userView = argUserView ?? UserView.new;

		backgroundCol = model.backgroundCol;
		this.setBackgroundCol(backgroundCol);

		// works for Views as well as Windows
		userView.onClose_({ model.removeDependant(this)});
	}

	setBackgroundCol { |color|
		userView.background_(color);
		^this
	}

	setDrawFunc {
		userView.drawFunc = {
			if(model.drawGridBool){
				drawingModel.drawGrid(
					width: gridLnWdth,
					color: gridLnCol,
					patterns: gridPatterns,
					dashPattern: model.dashPattern
				);
			};
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
		^this
	}

	update {|obj, what, val|
		var keys, changeBool;

		if(verbose) {
			format("GMITHGalleyViewModel has changed: %: %", what, val).inform;
		};

		case
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
		}
		{ what == \trigViewRefresh}{
			userView.refresh;
		}
		;
		^this
	}
}

// To be simplifed and made a subclass?
GMITHInsertModel {
	var screenBounds, <colRange, <cellData, scoreModel;
	var userView, <gridPatterns, <drawFunc, <drawGridBool;
	var <dashPattern, <gridLnCol, <gridLnWdth, curRows, curCols, gridDrawn=false;
	var <sqSize, scoreSelectModel, <>verbose = false, <eventsRange;
	var <selectedEvents, drawing, gridModel, <matrix, <drawEventsBool;
	var <fontSizeScale, <shapeLnWidth, <insetGrid, <backgroundCol;

	*new {arg scoreSelectModel, sqSize;
		^super.new.init(scoreSelectModel, sqSize);
	}

	init {|argScoreSelectModel, argSqSize|

		scoreSelectModel = argScoreSelectModel;
		scoreSelectModel.addDependant(this);
		sqSize = argSqSize;
		this.gridPatterns_([1], [1]);
		this.gridLnWdth_(1);
		this.shapeLnWidth_(3);
		this.gridLnCol_(Color.black);
		backgroundCol = Color.white;
		drawEventsBool = false;
		drawGridBool = false;
		fontSizeScale = 1.0;
		eventsRange = scoreSelectModel.colRange;
		selectedEvents = scoreSelectModel.cellData;
	}

	backgroundCol_ {|color|
		backgroundCol = color;
		this.changed(\backgroundCol, backgroundCol);
	}

	sqSize_ {arg float=10; // allow px also
		sqSize = float;
		this.changed(\sqSize, sqSize);
		if(gridDrawn){
			this.drawView(curRows, curCols);
		};
		this.changed(\trigViewRefresh);
		^this
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
		^this
	}

	gridLnWdth_ {|lineWdth|
		gridLnWdth = lineWdth;
		this.changed(\gridLnWdth, gridLnWdth);
		if(gridDrawn){
			this.drawView(curRows, curCols);
			this.changed(\trigViewRefresh);
		};
		^this
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
		^this
	}

	drawEvents_ { arg bool;
		drawEventsBool = bool;
		this.changed(\trigViewRefresh);
		^this
	}

	drawGrid_ { arg bool;
		drawGridBool = bool;
		this.changed(\trigViewRefresh);
		^this
	}

	drawGrid {
		^drawGridBool
	}

	drawEvents {
		^drawEventsBool
	}

	drawView { arg rows=3, cols = 20;
		var usrViewWidth, usrViewHeight;
		curRows = rows;
		// don't try and draw fewer cols than are currently in selection model
		curCols = cols.max(scoreSelectModel.numCols);
		gridDrawn = true;

		this.changed(\sqSize, sqSize);

		// number of dashes changes acc. to Pen.width, hence:
		dashPattern = FloatArray[
			(sqSize/14) * gridLnWdth.reciprocal,
			(sqSize/18) * gridLnWdth.reciprocal
		];

		usrViewWidth = (sqSize * curCols).ceil + gridLnWdth;
		usrViewHeight = (sqSize * curRows).ceil + gridLnWdth;

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
		this.changed(\drawing, drawing); // this triggers a redraw - check vs
		this.changed(\trigViewRefresh); // TODO - needed also?

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
			if(newNumCols != curCols){
				this.drawView(curRows, newNumCols);
			}{
				this.changed(\drawing, drawing); // this triggers recalc redraw data
				this.changed(\trigViewRefresh);
			}
		}
	}
}
