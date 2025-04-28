
/*
WinBlock
WinBlockModel : ViewCentralModel
WinBlockGui

© 2022 Tom Hall
www.ludions.com

*/


WinBlock {
	var <m, <dims, <win, <v, usrViewBool;

	*new { arg win, dims, margins, usrViewBool;
		^super.new.init(win, dims, margins, usrViewBool);
	}

	init { |aWin, aDims, aMargins, aUsrViewBool|
		dims = if(aDims.isNil, {[500, 300]}, {aDims});
		if(aMargins.isNil){
			aMargins = ((dims.sum/2) * 0.1).floor.asInteger
		};
		m = WinBlockModel.new(dims, aMargins);
		dims = m.dims;
		usrViewBool = aUsrViewBool;
		this.makeView(aWin, usrViewBool);
		^this
	}

	endFullScreen {
		m.endFullScreen;
		^this
	}

	fullScreen {
		m.fullScreen;
		^this
	}

	shrinkWin {m.shrinkWin; ^this } // shrink Window margins

	resizeWin {|x, y| m.resizeWin(x, y); ^this} //

	moveWin {|x, y| m.moveWin(x, y); ^this} //

	winDims {^m.winDims}

	margins { ^m.margins }

	margins_ {|args| m.margins_(args); ^this}

	view { ^v.view }

	viewDims { ^m.viewDims; }

	viewDims_ {|x, y| m.viewDims_(x, y); ^this}

	viewPosPx {^m.viewPosPx;}

	viewPosPx_ {|x, y| m.viewPosPx_(x, y); ^this }

	viewPos { ^m.viewPos;  }

	viewPos_ {|x, y| m.viewPos_(x, y); ^this }

	centreView { m.centreView; ^this}

	centerView { m.centreView; ^this}

	viewDimsPct {^m.viewDimsPct; }

	viewDimsPct_{|x, y| m.viewDimsPct_(x, y); ^this }

	snapshotPx { ^m.snapshotPx;  }

	snapshotPct { ^m.snapshotPct;  }

	restoreCols { m.restoreCols; ^this }

	winCol_ {|color| m.winCol_(color); ^this }

	winCol {^m.winCol }

	marginCol_ {|color| m.marginCol_(color); ^this }

	marginCol { ^m.marginCol }

	viewCol_ {|color| m.viewCol_(color); ^this }

	viewCol {^m.viewCol }

	close { m.close; ^this }

	newView {this.makeView(aUsrViewBool: usrViewBool); ^this}

	makeView { |aWin, aUsrViewBool|
		v = ViewCentral.new(aWin, m, aUsrViewBool);
		win = v.win;
		^this
	}

	gui {|scale=0.33|
		^WinBlockGui.new(m, scale);
	}
}



WinBlockModel : ViewCentralModel {

	var <viewSizeSpecs, <viewDimsPct, <viewPos, <viewPosSpecs;

	init { |aDims, aMargins, aUsrViewBool|

		super.init(aDims, aMargins, aUsrViewBool);

		// viewSizeSpecs specs should not change
		viewSizeSpecs = [[], []];
		viewSizeSpecs[0] = [0, dims[0]].asSpec;
		viewSizeSpecs[1] = [0, dims[1]].asSpec;

		viewDimsPct = this.calcViewDimsPct(viewDims);

		viewPosSpecs = this.calcViewPosSpecs(marginSums);
		viewPos = [0.5, 0.5]; 	// if margins 0, 0.5 initial default
		viewPos = this.calcViewPosPct(margins, viewPosSpecs);

		^this

	}

	calcViewPosSpecs {  |argMarginSums|
		var aViewPosSpecsArr;
		aViewPosSpecsArr = [[], []];
		// calculate the  view pos % from  margins
		aViewPosSpecsArr[0] = [0, argMarginSums[0]].asSpec;
		aViewPosSpecsArr[1] = [0, argMarginSums[1]].asSpec;
		^aViewPosSpecsArr
	}



	// pos %s from margins
	calcViewPosPct { |argMargins, argViewPosSpecs|
		var posX, posY;
		// if(curPos.isNil, {curPos = [0.5, 0.5] }); // avoid nils
		// calcs using current margins
		if (argViewPosSpecs.isNil, {
			argViewPosSpecs = this.calcViewPosSpecs(marginSums); // from viewMargSums
		});

		// only change pos if there _are_ currently margins > 0
		posX = if(argViewPosSpecs[0].range>0, {
			argViewPosSpecs[0].unmap(argMargins[0]);
		},{
			viewPos[0] // use old value if no X margin
		});

		posY = if(argViewPosSpecs[1].range>0, {
			1 - argViewPosSpecs[1].unmap(argMargins[1]);
		}, {
			viewPos[1] // use old value if no Y margin
		});
		^[posX, posY]
	}

	viewPosPx {^ [margins[0], margins[1]] }

	snapshotPx {^[viewDims, this.viewPosPx, dims]}

	snapshotPct {
		^[viewDimsPct, viewPos, dims]
	}


	calcViewDimsPct { |argViewSize|
		// viewSizeSpecs are fixed
		^[
			viewSizeSpecs[0].unmap(argViewSize[0]),
			viewSizeSpecs[1].unmap(argViewSize[1]),
		]
	}



	viewPosPx_{|x, y|
		var newMargins;
		if(x + viewDims[0] > dims[0]){
			x = marginSums[0];
			format("X pos clipped to %", x).postln;
		};
		if(y + viewDims[1] > dims[1]){
			y = marginSums[1];
			format("Y pos clipped to %", y).postln;
		};
		newMargins = [x, y, marginSums[0] -x, marginSums[1] -y];

		this.margins_(newMargins);

		^this;
	}

	prUpdateMargins { arg aMargins;
		aMargins = aMargins.max(0); // ensure no negative margins
		if (aMargins != margins, {
			margins = aMargins;
			this.changed(\margins, margins);
		}, {
			// "margins unchanged".warn;
		});
	}

	viewDims_ { arg x, y;

		var newMargins, newDims;
		var aViewMargArrX, aViewMargArrY;

		newDims = viewDims.copy;

		// allow input to either x or y if not both
		x = if(x.isNil, {viewDims[0]}, {x});
		y = if(y.isNil, {viewDims[1]}, {y});

		newDims = [x, y];

		if(newDims != viewDims, {

			// change dims instance var
			viewDims = newDims;
			// this.changed(\viewSize, viewDims); // TODO CHECK

			// update also
			viewDimsPct = this.calcViewDimsPct(viewDims);
			this.changed(\viewDimsPct, viewDimsPct);

			marginSums = (dims - newDims).max(0); // instance var

			viewPosSpecs = this.calcViewPosSpecs(marginSums);

			newMargins = this.calcMargsFromViewPos(*viewPos);

			this.prUpdateMargins(newMargins);

		});

		^this
	}

	// TODO consider: marginsDimsPct_

	// set viewSize as %s of view
	// Floats btn 0 ..1
	viewDimsPct_ {arg x, y;

		x = if(x.notNil, {
			viewSizeSpecs[0].map(x)
		},{
			// if nil, will be current val in viewDims_
			x // no change
		});

		y = if(y.notNil, {
			viewSizeSpecs[1].map(y)
		},{
			y // no change
		});

		this.viewDims_(x, y);
		^this
	}



	calcMargsFromViewPos { arg x, y; // %s
		var aViewMargArrX, aViewMargArrY, newMargins;

		// assumes viewPosSpecs is correct

		// calculate new X margin positions (pixels)
		aViewMargArrX = [
			viewPosSpecs[0].map(x),
			viewPosSpecs[0].map(1 - x)
		];

		// calculate new Y margin positions (pixels)
		aViewMargArrY = [
			viewPosSpecs[1].map(1 - y),
			viewPosSpecs[1].map(y)
		];

		// new margins
		// viewMargSums and viewSizeDims do not change

		newMargins = [
			aViewMargArrX[0],
			aViewMargArrY[0],
			aViewMargArrX[1],
			aViewMargArrY[1]
		];
		^newMargins
	}

	centreView { this.viewPos_(0.5, 0.5); ^this}

	centerView { this.centreView; ^this}

	viewPos_ { arg x, y; // %

		var newViewPos, newMargins;

		newViewPos = [x, y];

		// check for new pos change
		if(newViewPos != viewPos, {

			// update margins (viewMargSums do not change)
			newMargins = this.calcMargsFromViewPos(*newViewPos);

			// update margins - will do changed as needed
			this.prUpdateMargins(newMargins);

			viewPos = newViewPos;
			this.changed(\viewPos, viewPos);

		});
		^this
	}



	// enter as int or arr (of 2, or 4 values)
	margins_ { |argMargins|
		var newMargins, newViewPos, newViewSize, newMarginSums;

		argMargins = this.cleanMarginsFormat(argMargins);

		if ( argMargins != margins, {

			this.prUpdateMargins(argMargins);
			newMarginSums = this.calcViewMargSums(margins);

			if(newMarginSums != marginSums, {
				marginSums = newMarginSums;
				viewDims = this.calcViewDims(margins);
				this.changed(\viewDims, viewDims);

				viewDimsPct = this.calcViewDimsPct(viewDims);
				this.changed(\viewDimsPct, viewDimsPct);

				viewPosSpecs = this.calcViewPosSpecs(marginSums);
			});

			newViewPos = this.calcViewPosPct(margins, viewPosSpecs);

			if(newViewPos != viewPos, {
				viewPos = newViewPos;
				this.changed(\viewPos, viewPos);
			});
		});
		^this
	}









}


WinBlockGui {
	var model, <window, slider2D, sliderX, sliderY, button, scale, layout, verbose;

	*new { |model, scale=0.33, verbose = true|
		^super.new.init(model, scale, verbose);
	}

	init { |argModel, argScale, argVerbose|
		model = argModel;
		model.addDependant(this);
		scale = argScale;
		verbose = argVerbose;

		sliderY = Slider.new;
		sliderX = Slider.new.orientation_(\horizontal);
		sliderY.thumbSize;
		sliderX.thumbSize;
		button = Button.new;
		button.string_("C"); // centre

		// avoid larger rectangular button
		button.maxWidth_(button.sizeHint.height;);

		window = Window.new(bounds: Rect(
			200,
			200,
			(model.dims[0] * scale).round + sliderY.thumbSize,
			(model.dims[1] * scale).round + sliderY.thumbSize
		));

		window.layout_(layout = GridLayout.rows(
			[slider2D = Slider2D(), sliderY],
			[sliderX, button]
		)).front;

		layout.minColumnWidth(0).postln;
		layout.minColumnWidth(1).postln;

		// change view position
		slider2D.action = { |view|
			model.viewPos_(view.x, view.y);
			if(verbose){
				//[view.x, view.y].postln;
				model.viewPos.round(0.01).postln;
			}
		};

		sliderX.action = { |view|
			// change viewSize width
			model.viewDimsPct_(x: view.value);
			if(verbose){
				model.viewDimsPct.round(0.01).postln;
			}
		};

		sliderY.action = { |view|
			// change viewSize height
			model.viewDimsPct_(y: view.value);
			if(verbose){
				model.viewDimsPct.round(0.01).postln;
			}
		};

		button.action = {|view|
			// centre view margins
			model.centreView;
			if(verbose){
				model.viewPos.round(0.01).postln;
			}
		};

		// set initial values
		this.update(model, \viewPos, model.viewPos);
		this.update(model, \viewDimsPct, model.viewDimsPct);

		window.front;
		window.onClose_({ model.removeDependant(this)});
		^window // rtn win
	}

	update {|obj, what, val|
		case
		{what == \viewPos} {
			slider2D.setXY(model.viewPos[0], model.viewPos[1]);
		}
		{what == \viewDimsPct } {
			sliderX.value = model.viewDimsPct[0];
			sliderY.value = model.viewDimsPct[1];
		};
		^this
	}
}


