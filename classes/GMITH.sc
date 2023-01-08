
/*
© 2022 Tom Hall

*/


GMITH {
	var <score, <gv, <page;

	*new {
		^super.new.init;
	}

	init {
		score = GMITHScore.new;
		gv = Dictionary.new;
		page = Dictionary.new;
		^this
	}

	moveAll {|int|
		^score.moveAll(int)
	}

	scoreArr {
		^score.scoreArr
	}

	scoreDict {
		^score.scoreDict
	}

	addDict {|dict|
		^score.addDict(dict)
	}

	add {|assoc, update = true|
		score.add(assoc, update);
		^this
	}

	undo {
		score.undo;
		^this
	}

	addAll {|arr|
		score.addAll(arr);
		^this
	}

	remove {|event|
		if(event.isNil){
			event = score.mouseSelect
		};
		score.remove(event);
		^this
	}

	removeAll {
		score.removeAll;
		^this
	}

	addMouseCell {|events, update = true|
		score.addMouseCell(events, update)
		^this
	}

	removeMouseCell {|update = true|
		score.removeMouseCell(update);
		^this
	}

	move {|keyFrom, keyTo|
		score.move(keyFrom, keyTo);
		^this
	}

	mouseMove {
		score.mouseMove;
		^this
	}


	postEvents {|order=0|
		score.postEvents(order);
		^this
	}

	cellEventKey {|key|
		^score.cellEventKey(key)
	}

	cellEvent {|key|
		^score.cellEvent(key)
	}

	association {|key|
		^score.association(key)
	}

	mouseSelect {
		^score.mouseSelect
	}

	eventSelect {
		^score.eventSelect // via mouseSelect
	}

	addedEvent {
		^score.addedEvent
	}

	removedEvent {
		^score.removedEvent
	}


	makePage { |key, numSystems, dims, titleBool = false, win|
		var view, wbMod, nfMod, nfView;
		// new page
		if(page[key].isNil){
			page.add(
				key -> Dictionary[\wbm->WinBlockModel.new(dims, usrViewBool: false)]
			);
			page[key].add(\titleBool->titleBool);
			nfMod = if(titleBool){
				NotnFlowTitleModel.new(page[key][\wbm], numSystems)
			}{
				NotnFlowModel.new(page[key][\wbm], numSystems)
			};
			page[key].add(\nfm->nfMod);
		}{
			// page is being remade
			if(titleBool != page[key][\titleBool]){
				"Page type / titleBool cannot be changed using same key".error;
				^this
			};
			//nfMod = page[key][\nfm];
		};

		wbMod = page[key][\wbm];
		// overwrite existing WinBlock model if dims has changed
		if(dims.notNil and:{ dims != wbMod.dims}){
			page[key].add(\wbm->WinBlockModel.new(dims, usrViewBool: false));
			wbMod = page[key][\wbm];
			"New dimension, please reset margins as needed".postln
		};
		nfMod = page[key][\nfm];
		if(numSystems.notNil and:{ numSystems != nfMod.numSystems}){
			"numSystems cannot be changed using same key".error;
			^this
		};
		view = ViewCentral.new(win, wbMod);
		view.moveWin(20, 20);
		nfView = if(titleBool){
			NotnFlowTitle.new(nfMod, view.view);
		}{
			NotnFlow.new(nfMod, view.view);
		};

		this.makePageSystemViews(key, numSystems, nfView);

		^[nfView.win, win]
		//^view
	}

	makePageSystemViews {|key, numSystems, nfView|
		var cellModels, systemInsertModels, cellHeightGuess;
		var systemInsertViews;

		// Cell Selection Model
		if(page[key][\csm].isNil){

			cellModels = numSystems.collect{
				GMITHCellSelectionModel(score)
			};
			page[key].add(\csm->cellModels);
		}{
			cellModels = page[key][\csm]
		};

		if(page[key][\sim].isNil){

			// assumes 3 cols, can be changed later in model
			cellHeightGuess = nfView.systemNotns[0].bounds.height/3;

			systemInsertModels = numSystems.collect{|i|
				GMITHInsertModel.new(cellModels[i], cellHeightGuess);
			};
			systemInsertModels.do{|i|
				i.drawGrid_(true);
				i.drawEvents_(true);
			};
			page[key].add(\sim->systemInsertModels);
		}{
			systemInsertModels = page[key][\sim]
		};

		systemInsertViews = numSystems.collect{|i|
			// GMITHInsertView(page[key][\sim][i], nfView.systemNotns[i], verbose: false);
			GMITHInsertView(systemInsertModels[i], nfView.systemNotns[i], verbose: false);
		};

		^this
	}

	pageDrawSystem {|key, system, startCol, endCol, numRows = 3|
		var numCols;
		this.pageCellsColRange_(key, system, startCol, endCol);
		numCols = this.pageCellsNumCols(key, system);
		this.pageSysInsModel(key, system).drawView(numRows, numCols);
		^this
	}

	pageNumSystems {|key|
		// TODO use tryPerform
		^this.pageNFModel(key).numSystems
	}

	pageSysSqSize_ {|key, size|
		var numSys;
		numSys = this.pageNumSystems(key);
		numSys.do{|i|
			// TODO use tryPerform
			this.pageSysInsModel(key, i).sqSize_(size);
		};
		^this
	}

	pageSysSqSize {|key|
		// TODO use tryPerform
		^this.pageSysInsModel(key, 0).sqSize
	}

	pageGridLnWdth_ {|key, width|
		var numSys;
		numSys = this.pageNumSystems(key);
		numSys.do{|i|
			// TODO use tryPerform
			this.pageSysInsModel(key, i).gridLnWdth_(width);
		};
		^this
	}

	pageGridLnWdth {|key|
		// TODO use tryPerform
		^this.pageSysInsModel(key, 0).gridLnWdth
	}


	pageShapeLnWidth_ {|key, width|
		var numSys;
		numSys = this.pageNumSystems(key);
		numSys.do{|i|
			// TODO use tryPerform
			this.pageSysInsModel(key, i).shapeLnWidth_(width);
		};
		^this
	}

	pageShapeLnWidth {|key|
		// TODO use tryPerform
		^this.pageSysInsModel(key, 0).shapeLnWidth
	}


	pageGridPatterns_ {|key, x, y|
		var numSys;
		numSys = this.pageNumSystems(key);
		numSys.do{|i|
			// this.pageSysInsModel(key, i).gridPatterns_(x, y)
			this.pageSysInsModel(key, i).tryPerform(\gridPatterns_, x, y)
		};
		^this
	}

	pageFontSizeScale {|key|
		^this.pageSysInsModel(key, 0).tryPerform(\fontSizeScale)
	}

	pageFontSizeScale_ {|key, val|
		var numSys;
		numSys = this.pageNumSystems(key);
		numSys.do{|i|
			this.pageSysInsModel(key, i).tryPerform(\fontSizeScale_, val);
		};
		^this
	}

	pageGridPatterns {|key|
		^this.pageSysInsModel(key, 0).tryPerform(\gridPatterns)
	}

	pageSysInsModel {|key, system=0|
		^page[key][\sim][system]
	}

	pageCellSelModels {|key|
		^page[key][\csm]; // Array
	}

	pageCellsColRange {|key, system=0|
		^this.pageCellSelModels(key)[system].colRange
	}

	pageCellsColRange_ {|key, system=0, startCol, endCol|
		^this.pageCellSelModels(key)[system].colRange_(startCol, endCol)
	}

	pageCellsNumCols {|key, system=0|
		^this.pageCellSelModels(key)[system].numCols
	}

	scoreCellsColRange {|startAt0Bool = true|
		^score.cellsColRange(startAt0Bool);
	}

	scoreCellsNumCols {|startAt0Bool = true|
		^score.cellsNumCols(startAt0Bool);
	}

	pageParam {|key, param|
		^this.pageNFModel(key).tryPerform(param);
	}

	pageSysTexts {|key|
		^this.pageParam(key, \systemTxts)
	}

	pageViewCol {|key|
		^this.pageNFModel(key).tryPerform(\viewCol)
	}

	pageViewCol_ {|key, color|
		this.pageNFModel(key).tryPerform(\viewCol_, color);
		^this
	}

	// see also pageColor below

	setPageSysTexts {|key, first, subseq, font, color|
		this.pageNFModel(key).tryPerform(\setSystemTxts, first, subseq, font, color);
		^this;
	}

	pageGui {|key|
		this.pageMarginsGui(key);
		^"pageGui depricated, use pageMarginsGui";
	}

	pageMarginsGui {|key|
		^WinBlockGui.new(this.pageWBModel(key));
	}

	pageMargins_ {|key, margins|
		^this.pageWBModel(key).tryPerform(\margins_, margins);
	}

	pageMargins {|key|
		^this.pageWBModel(key).tryPerform(\margins);
	}

	pageViewDims_ {|key, viewX, viewY|
		^this.pageWBModel(key).tryPerform(\viewDims_, viewX, viewY);
	}

	pageViewDims {|key|
		^this.pageWBModel(key).tryPerform(\viewDims);
	}

	pageViewDimsPct_ {|key, viewX, viewY|
		^this.pageWBModel(key).tryPerform(\viewDimsPct_, viewX, viewY);
	}

	pageViewDimsPct {|key|
		^this.pageWBModel(key).tryPerform(\viewDimsPct);
	}

	pageDims_ {|key, viewX, viewY|
		^"pageDims cannot be altered".error
	}

	pageDims {|key|
		^this.pageWBModel(key).tryPerform(\dims);
	}

	pageCenterView {|key|
		^this.pageCentreView(key)
	}

	pageCentreView {|key|
		^this.pageWBModel(key).tryPerform(\centreView);
	}

	pageViewModel  {|key|
		^this.pageNFModel(key)
	}

	pageNFModel {|key|
		if(page[key].notNil){
			^page[key][\nfm]
		}{
			^"Please input a valid key".error;
		}
	}

	pageWBModel {|key|
		if(page[key].notNil){
			^page[key][\wbm]
		}{
			^"Please input a valid key".error;
		}
	}

	pagefullScreen {|key|
		^this.pageWBModel(key).tryPerform(\fullScreen)
	}

	pageEndFullScreen {|key|
		^this.pageWBModel(key).tryPerform(\endFullScreen)
	}

	pageWinCol {|key|
		^this.pageWBModel(key).tryPerform(\winCol)
	}

	pageWinCol_ {|key, color|
		this.pageWBModel(key).tryPerform(\winCol_, color)
	}

	pageMarginCol {|key|
		^this.pageWBModel(key).tryPerform(\marginCol)
	}

	pageMarginCol_ {|key, color|
		this.pageWBModel(key).tryPerform(\marginCol_, color);
		//  == lineSpacingCol
		this.pageWBModel(key).tryPerform(\viewCol_, color);
		^this
	}

	pageColor_ {|key, color|
		this.pageMarginCol_(key, color);
		this.pageViewCol_(key, color);
		^this
	}

	pageColor {|key|
		//this.pageMarginCol(key);
		^this.pageViewCol(key);
	}

	// TODO check this, method does nothing ATM
	// pageRestoreCols {|key|
	// 	^this.pageWBModel(key).tryPerform(\restoreCols)
	// }

	makeGalley {|key, rows, cols, vHeightPct, win|
		var view, galMod, selMod;
		if(gv[key].isNil){
			gv.add(
				key -> Dictionary[\sel->GMITHCellSelectionModel(score)]
			);
			if(vHeightPct.isNil){
				vHeightPct = 0.15
			};
			gv[key].add(\mod->GMITHGalleyViewModel.new(gv[key][\sel], vHeightPct));

			if(cols.isNil){
				cols = if(score.cellsColRange.notNil){
					score.cellsColRange[1] - score.cellsColRange[0]
				}{
					0
				};
			}{
				cols = cols -1;
			};
			gv[key][\sel].colRange_(0, cols);
		}{
			// key exists already
			if(cols.notNil){
				gv[key][\sel].colRange_(0, cols-1);
			};
			if(vHeightPct.notNil){
				gv[key][\mod].maxHeightPct_(vHeightPct);
			};
		};
		// TODO this should be part of the select class
		if(rows.isNil){
			rows = score.cellsNumRows.max(3)
		};
		rows = rows.max(1);
		galMod = gv[key][\mod];
		galMod.drawEvents_(true);
		selMod = gv[key][\sel];
		view = GMITHGalleyViewWindow(galMod, win, verbose: false);
		this.gvColRange_(key, selMod.colRange[0], selMod.colRange[1]);
		galMod.drawGrid(rows, selMod.numCols);
		^view
	}

	gvModel {|key|
		if(gv[key].notNil){
			^gv[key][\mod]
		}{
			"key is not valid".error;
			^nil
		}
	}

	gvSelModel {|key|
		if(gv[key].notNil){
			^gv[key][\sel]
		}{
			"key is not valid".error;
			^nil
		}
	}


	gvShapeLnWidth {|key|
		^this.gvModel(key).tryPerform(\shapeLnWidth)
	}

	gvShapeLnWidth_ {|key, val|
		this.gvModel(key).tryPerform(\shapeLnWidth_, val)
		^this
	}

	gvGridPatterns {|key|
		^this.gvModel(key).tryPerform(\gridPatterns)
	}

	gvGridPatterns_ {|key, x, y|
		this.gvModel(key).tryPerform(\gridPatterns_, x, y)
		^this
	}

	gvGridLineWidth {|key|
		^this.gvModel(key).tryPerform(\gridLnWdth)
	}

	gvGridLineWidth_ {|key, val|
		^this.gvModel(key).tryPerform(\gridLnWdth_, val)
	}


	gvFontSizeScale {|key|
		^this.gvModel(key).tryPerform(\fontSizeScale)
	}

	gvFontSizeScale_ {|key, val|
		this.gvModel(key).tryPerform(\fontSizeScale_, val);
		^this
	}

	gvMaxHeightPct {|key|
		^this.gvModel(key).tryPerform(\maxHeightPct)
	}

	gvMaxHeightPct_ {|key, val|
		this.gvModel(key).tryPerform(\maxHeightPct_, val);
		^this
	}

	gvColRange {|key|
		^this.gvSelModel(key).tryPerform(\colRange)
	}

	gvColRange_ {|key, a=0, b|
		if(b.isNil){
			b = if(score.cellsColRange.isNil){
				0
			}{
				score.cellsColRange[1]
			};
		};
		this.gvSelModel(key).tryPerform(\colRange_, a, b);
		^this
	}


}


