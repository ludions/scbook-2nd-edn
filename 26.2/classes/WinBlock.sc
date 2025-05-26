
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
		win.front;
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
			// if neither WinBlock nor model have this selected method
			^super.doesNotUnderstand(selector, *args)
		}
	}

}



WinBlockModel : ViewCentralModel {

	var <viewSizeSpecs, <viewDimsPct, <viewPosPct, <viewPosPctSpecs;

	init { |aDims, aMargins, aUsrViewBool|

		super.init(aDims, aMargins, aUsrViewBool);

		// viewSizeSpecs specs should not change
		viewSizeSpecs = [[], []];
		// x-axis is 0 to window width
		viewSizeSpecs[0] = [0, dims[0]].asSpec;
		// y-axis is 0 to window height
		viewSizeSpecs[1] = [0, dims[1]].asSpec;

		viewDimsPct = this.calcViewDimsPct(viewDims);

		// marginSums are caclulated in parent method margins_
		viewPosPctSpecs = this.calcViewPosSpecs(marginSums);
		viewPosPct = [0.5, 0.5]; 	// if margins 0, 0.5 initial default
		viewPosPct = this.calcViewPosPct(margins, viewPosPctSpecs);

		^this

	}

	// makes specs that map 0-1.0 to margin dims
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

		// calcs using current margins
		if (argViewPosSpecs.isNil, {
			argViewPosSpecs = this.calcViewPosSpecs(marginSums); // from viewMargSums
		});

		// only change pos if there _are_ currently margins > 0
		posX = if(argViewPosSpecs[0].range>0, {
			argViewPosSpecs[0].unmap(argMargins[0]);
		},{
			viewPosPct[0] // use old value if no X margin
		});

		// calc Y pos (inverted)
		posY = if(argViewPosSpecs[1].range>0, {
			1 - argViewPosSpecs[1].unmap(argMargins[1]);
		}, {
			viewPosPct[1] // use old value if no Y margin
		});
		^[posX, posY]
	}

	viewPos {^ [margins[0], margins[1]] }


	snapshot {
		"// [viewDims, viewPos, dims, winPos]".postln;
		^[viewDims, this.viewPos, dims, this.winPos.asInteger]
	}

	snapshotPct {
		"// [viewDimsPct, viewPosPct, dims, winPos]".postln;
		^[viewDimsPct.round(0.001), viewPosPct.round(0.001), dims, this.winPos.asInteger]
	}

	calcViewDimsPct { |argViewSize|
		// viewSizeSpecs are fixed
		^[
			viewSizeSpecs[0].unmap(argViewSize[0]),
			viewSizeSpecs[1].unmap(argViewSize[1]),
		]
	}

	viewPos_{|x, y|
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

	validateMargins { arg aMargins;
		aMargins = aMargins.max(0); // ensure no negative margins
		if (aMargins != margins, {
			margins = aMargins;
			this.changed(\margins, margins);
		}, {
			// "margins unchanged".warn;
		});
	}

	viewDims_ { |x, y|
		var newDims, newMargins;

		x = x ?? viewDims[0];
		y = y ?? viewDims[1];
		newDims = [x, y];

		if(newDims != viewDims) {
			// set viewDims first
			viewDims = newDims;

			// update %s
			viewDimsPct = this.calcViewDimsPct(viewDims);
			this.changed(\viewDimsPct, viewDimsPct);

			// calc new margin sums
			marginSums = (dims - viewDims).max(0);
			viewPosPctSpecs = this.calcViewPosSpecs(marginSums);

			// calc margins that maintain pos %
			newMargins = this.calcMarginsFromViewPos(*viewPosPct);

			// Update margins (but viewDims is already set)
			this.validateMargins(newMargins);
		}
		^this
	}

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


	calcMarginsFromViewPos { |x, y|  // %s
		var leftMargin, topMargin, rightMargin, bottomMargin;

		// assumes viewPosPctSpecs is correct
		// calc horizontal margins
		leftMargin = viewPosPctSpecs[0].map(x);
		rightMargin = viewPosPctSpecs[0].map(1 - x);

		// calc vertical margins (Y inverted)
		topMargin = viewPosPctSpecs[1].map(1 - y);
		bottomMargin = viewPosPctSpecs[1].map(y);

		^[leftMargin, topMargin, rightMargin, bottomMargin]
	}

	centreView {
		this.viewPosPct_(0.5, 0.5);
		^this
	}

	centerView {
		this.centreView;
		^this
	}

	viewPosPct_ { arg x, y; // %
		var newViewPosPct, newMargins;
		newViewPosPct = [x, y];
		// check for new pos change
		if(newViewPosPct != viewPosPct, {

			// update margins (viewMargSums do not change)
			newMargins = this.calcMarginsFromViewPos(*newViewPosPct);

			// update margins without triggering parent's recalcs
			// - will do changed as needed
			this.validateMargins(newMargins);

			viewPosPct = newViewPosPct;
			this.changed(\viewPosPct, viewPosPct);

		});
		^this
	}

	margins_ { |argMargins|
		var oldMarginSums, oldMargins;
		oldMargins = margins;
		oldMarginSums = marginSums;

		// class parent makes initial changes
		super.margins_(argMargins);

		// if margins actually changed, make changes to Pct info
		if(margins != oldMargins) {
			if(marginSums != oldMarginSums) {
				// Update position specs when margin space changes
				viewPosPctSpecs = this.calcViewPosSpecs(marginSums);
			};

			// update view %s
			viewDimsPct = this.calcViewDimsPct(viewDims);
			this.changed(\viewDimsPct, viewDimsPct);

			// recalc pos %s
			viewPosPct = this.calcViewPosPct(margins, viewPosPctSpecs);
			this.changed(\viewPosPct, viewPosPct);
		};
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
			model.viewPosPct_(view.x, view.y);
			this.postVerbose("View Pos", model.viewPosPct);
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
			this.postVerbose("View Pos", model.viewPosPct);
		};

		// set initial values
		this.update(model, \viewPosPct, model.viewPosPct);
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
		{what == \viewPosPct} {
			slider2D.setXY(model.viewPosPct[0], model.viewPosPct[1]);
		}
		{what == \viewDimsPct } {
			sliderX.value = model.viewDimsPct[0];
			sliderY.value = model.viewDimsPct[1];
		};
		^this
	}
}


