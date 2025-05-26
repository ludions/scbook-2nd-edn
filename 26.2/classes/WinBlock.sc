
/*
WinBlock
WinBlockModel
WinBlockGui

Dependency: ViewCentralModel

© 2022–2025 Tom Hall
www.ludions.com

*/


WinBlock {
	var <model, <win, <viewCentral, <usrViewBool;

	*new { arg win, dims, margins, usrViewBool;
		^super.new.init(win, dims, margins, usrViewBool);
	}

	init { |aWin, aDims, aMargins, aUsrViewBool|
		usrViewBool = aUsrViewBool;
		model = WinBlockModel.new(aDims, aMargins);
		this.makeView(aWin, usrViewBool);
		^this
	}

	view { ^viewCentral.view }  // the View / UserView

	// remake if the main view is closed
	newView {
		this.makeView;
		^this
	}

	makeView { |aWin|
		viewCentral = ViewCentral.new(aWin, model, usrViewBool);
		win = viewCentral.win;
		^this
	}

	gui {|scale=0.33|
		^WinBlockGui.new(model, scale);
	}

	// delegate user methods to model implementation
	doesNotUnderstand { |selector ... args|
		var result;
		// check if the model responds to this selector
		if(model.respondsTo(selector)) {
			// forward message to the model
			result = model.performList(selector, args);

			// does the model returns itself?
			if(result === model) {
				^this
			} {
				^result
			}
		} {
			// neither WinBlock nor model have this method
			^super.doesNotUnderstand(selector, *args)
		}
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
		var winBounds;
		model = argModel;
		model.addDependant(this);
		scale = argScale.clip(0.2, 1); // sensible values
		verbose = argVerbose;

		slider2D = Slider2D();
		sliderY = Slider.new;
		sliderX = Slider.new.orientation_(\horizontal);
		button = Button.new;
		button.string_("⊕"); // centre
		// avoid larger rectangular button
		button.maxWidth_(button.sizeHint.height);


		layout = GridLayout.rows(
			[slider2D, sliderY],
			[sliderX, button]
		);

		// set columns stretch
		layout.setColumnStretch(0, 1);  // main column stretches
		layout.setColumnStretch(1, 0);  // side column is fixed

		winBounds = this.calcWindowBounds(model.dims, scale, sliderY.thumbSize);
		window = Window.new(bounds:winBounds);
		window.layout_(layout).front;

		// change view position
		slider2D.action = { |view|
			model.viewPos_(view.x, view.y);
			this.postVerbose("View Pos", model.viewPos);
		};

		sliderX.action = { |view|
			// change viewSize width
			model.viewDimsPct_(x: view.value);
			this.postVerbose("View Pct", model.viewDimsPct);
		};

		sliderY.action = { |view|
			// change viewSize height
			model.viewDimsPct_(y: view.value);
			this.postVerbose("View Pct", model.viewDimsPct);
		};

		button.action = {|view|
			// centre view margins
			model.centreView;
			this.postVerbose("View Pos", model.viewPos);
		};

		// set initial values
		this.update(model, \viewPos, model.viewPos);
		this.update(model, \viewDimsPct, model.viewDimsPct);

		window.front;
		window.onClose_({ model.removeDependant(this)});
		^window // rtn win
	}

	postVerbose { |label, value, precision = 0.01|
		if(verbose) {
			format("% : %", label, value.round(precision)).postln;
		}
	}

	calcWindowBounds { |modelDims, scale, thumbSize|
		var scaledWidth, scaledHeight, totalWidth;
		var totalHeight, screenBounds, left, top;
		scaledWidth = (modelDims[0] * scale).round;
		scaledHeight = (modelDims[1] * scale).round;
		totalWidth = scaledWidth + thumbSize;
		totalHeight = scaledHeight + thumbSize;

		// place gui bottom left of screen
		screenBounds = Window.screenBounds;
		left = (screenBounds.width - totalWidth) / 5;
		top = (screenBounds.height - totalHeight) / 4;

		^Rect(left, top, totalWidth, totalHeight)
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


