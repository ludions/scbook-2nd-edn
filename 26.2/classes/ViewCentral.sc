
/*
ViewCentral
ViewCentralModel

© 2022-2025 Tom Hall

*/

ViewCentral {
	var <win, <view, <model, <marginView;
	var dims, winCol, margins, marginCol;
	var usrViewBool, viewCol, winName;

	*new { arg win, model;
		^super.new.init(win, model)
	}

	init { arg aWin, aModel;

		win = aWin ?? {Window.new.front};
		model = aModel;
		model.addDependant(this);

		usrViewBool = model.usrViewBool;

		dims = model.dims; // these remain a fixed size

		// for when view is remade
		if(model.winPos.notNil){
			var pos = model.winPos;
			this.applyWinPos(pos[0], pos[1])
		};

		this.registerWinAction(win);

		this.registerKeyDownAction(model.escKey);

		win.onClose_({
			win.endFrontAction = {};
			model.removeDependant(this);
		});

		// becomes a margin to the view
		marginView= View.new(win).fixedSize_(Size(dims[0], dims[1]));

		// default for using Pen in the view
		view = if(usrViewBool, {
			UserView.new(marginView, marginView.bounds)
		},{
			View.new(marginView, marginView.bounds)
		});

		this.makeLayout;

		this.applyMarginCol_(model.marginCol);
		this.applyViewCol_(model.viewCol);
		this.applyWinCol_(model.winCol);

		// kludge, win won't resize immediately,
		// maybe problem with layouts?
		r{
			0.0001.wait;
			this.applyWinSize(*model.winDims);
		}.play(AppClock);

		^this
	}

	registerKeyDownAction { |escKey|
		win.view.keyDownAction_({
			arg view, char, modifiers, unicode, keycode, key;
			if(keycode==escKey, {win.endFullScreen});
		});
		^this
	}

	registerWinAction { |win|
		var yPos, screenHeight, winBorder = 28, winBounds;
		screenHeight = this.getScreenHeight;
		win.endFrontAction = {
			if(win.isClosed.not){
				winBounds = win.bounds;
				yPos = screenHeight - winBounds.top - winBounds.height - winBorder;
				model.winPos = [winBounds.left, yPos]; // CONTROLER TO MODEL
				// ["model winPos changed", winBounds.left, yPos].postln; // testing
			}
		}
		^this
	}

	getScreenHeight {
		^Window.availableBounds.height;
	}

	makeLayout {
		win.layout = VLayout([
			marginView.layout_(
				HLayout(view)
			),
			align: \center
		]);

		this.applyMargins_(model.margins);
		win.layout.margins_(0); // window margins
		^this
	}

	// for use via model
	applyWinSize { |x, y|
		win.setInnerExtent(x, y);
		^this
	}

	// for use via model
	applyWinPos { |x, y|
		var bounds, rect;
		bounds = win.bounds;
		rect = Rect(x, y, bounds.width, bounds.height);
		win.setTopLeftBounds(rect, menuSpacer: 45);
		^this
	}

	// for use via model
	applyMarginCol_ { |color|
		marginCol = color;
		marginView.background_(marginCol);
		^this
	}

	// for use via model
	applyViewCol_ { |color|
		viewCol = color;
		view.background_(viewCol);
		^this
	}

	// for use via model
	applyWinCol_ { |color|
		winCol =  color;
		win.background_(winCol);
		^this
	}

	// for use via model
	applyWinName_{ |name|
		winName = name;
		win.name_(winName);
		^this
	}

	// for use via model
	applyMargins_ { |argMargins|
		margins = argMargins;
		marginView.layout.margins = margins; // LTRB
		^this
	}

	// for use via model
	// remove all views from the window
	applyRemove {
		if(win.isClosed.not, {
			view.remove;
			marginView.remove;
		});
		^this;
	}

	// for use via model
	applyFullScreen {
		win.fullScreen;
		^this
	}

	// for use via model
	applyFront {
		win.front;
		^this
	}

	// for use via model
	applyEndFullScreen {
		win.endFullScreen;
		^this
	}

	// for use via model
	applyClose {
		win.close;
		^this
	}

	update {|obj, what, val|
		case{what == \margins} {
			this.applyMargins_(val) // LTRB
		}
		{what == \winDims} {
			this.applyWinSize(*val)
		}
		{what == \winPos} {
			this.applyWinPos(*val)
		}
		{what == \winCol} {
			this.applyWinCol_(val)
		}
		{what == \viewCol} {
			this.applyViewCol_(val)
		}
		{what == \marginCol} {
			this.applyMarginCol_(val)
		}
		{what == \winName} {
			this.applyWinName_(val)
		}
		{what == \close} {
			this.applyClose
		}
		{what == \keyDownAction} {
			this.registerKeyDownAction(val)
		}
		{what == \removeViews} {
			this.applyRemove
		}
		{what == \fullScreen} {
			this.applyFullScreen
		}
		{what == \front} {
			this.applyFront
		}
		{what == \endFullScreen} {
			this.applyEndFullScreen
		}
		^this
	}
}


ViewCentralModel {

	var <dims, <margins, <>winDims, <usrViewBool, <viewDims;
	var <winCol, <viewCol, <marginCol, <marginSums, <>winPos;
	var <screenHeight, <winName = "", <escKey;

	*new { arg dims, margins, usrViewBool;
		^super.new.init(dims, margins, usrViewBool);
	}

	init { |aDims, aMargins, aUsrViewBool|
		dims = aDims ?? [620, 460]; // NB not view
		winDims = dims;
		margins = aMargins ?? (20!4);
		margins = this.cleanMarginsFormat(margins);
		marginSums = this.calcViewMargSums(margins);
		viewDims = this.calcViewDims(margins);
		escKey = 53; // MBP
		usrViewBool = aUsrViewBool ?? true;

		winCol = Color.grey(0.75);
		marginCol = Color.grey(0.9);
		viewCol = Color.white;

		^this
	}

	escKey_ { |int = 53|
		escKey = int;
		this.changed(\keyDownAction, escKey);
		^this
	}

	restoreCols {
		this.winCol_(Color.grey(0.75));
		this.marginCol_(Color.grey(0.9));
		this.viewCol_(Color.white);
		^this;
	}

	removeViews {
		this.changed(\removeViews);
		^this
	}

	calcViewDims { |argMargins|
		^(dims - this.calcViewMargSums(argMargins))
	}

	calcViewMargSums { |argMargins|
		^[[argMargins[0], argMargins[2]].sum,
			[argMargins[1], argMargins[3]].sum
		]
	}

	winName_{ |string|
		winName = string;
		this.changed(\winName, string);
		^this
	}

	shrinkWin {
		this.resizeWin(*dims);
		^this;
	}

	resizeWin { |x, y|
		winDims = [x, y];
		this.changed(\winDims, [x, y]);
	}

	moveWin { |x=0, y=0|
		winPos = [x, y];
		this.changed(\winPos, [x, y]);
	}

	endFullScreen {
		this.changed(\endFullScreen);
		^this
	}

	fullScreen {
		this.changed(\fullScreen);
		^this
	}

	front {
		this.changed(\front);
		^this
	}

	close {
		this.changed(\close);
		^this
	}

	winCol_ { |color|
		winCol = color;
		this.changed(\winCol, winCol);
		^this
	}

	viewCol_ { |color|
		viewCol = color;
		this.changed(\viewCol, viewCol);
		^this
	}

	marginCol_ { |color|
		marginCol = color;
		this.changed(\marginCol, marginCol);
		^this
	}

	margins_ { |argMargins|
		margins = this.cleanMarginsFormat(argMargins); // LTRB
		this.changed(\margins, margins);

		// mostly used in subclasses
		marginSums = this.calcViewMargSums(margins);
		this.changed(\marginSums, marginSums);
		viewDims = this.calcViewDims(margins);
		this.changed(\viewDims, viewDims);

		^this
	}

	cleanMarginsFormat { |argMargins |
		var newMargins;
		newMargins = case {argMargins.isNil} {25 ! 4}
		{argMargins.isKindOf(SimpleNumber)} {argMargins ! 4}
		{argMargins.size == 2} {
			[argMargins[0], argMargins[1], argMargins[0], argMargins[1]];
		}
		{argMargins.size == 4} { argMargins };
		^newMargins.max(0);
	}
}


