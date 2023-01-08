
/*
ViewCentral
ViewCentralModel

© 2022 Tom Hall

*/


ViewCentralModel {

	var <dims, <margins, <>winDims, <usrViewBool, <viewDims;
	var <winCol, <viewCol, <marginCol, <marginSums, <>winPos;
	var <screenHeight, <winName = "", <escKey;

	*new { arg dims, margins, usrViewBool;
		^super.new.init(dims, margins, usrViewBool);
	}

	init { |aDims, aMargins, aUsrViewBool|
		dims = aDims; // NB not view
		winDims = dims;
		margins = aMargins ?? (25!4);
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

	escKey_ {|int|
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

	calcViewDims {  |argMargins|
		^(dims - this.calcViewMargSums(argMargins))
	}

	calcViewMargSums { |argMargins|
		^[[argMargins[0], argMargins[2]].sum,
			[argMargins[1], argMargins[3]].sum
		]
	}

	winName_{|string|
		winName = string;
		this.changed(\winName, string);
	}

	shrinkWin {
		this.resizeWin(*dims);
		^this;
	}

	resizeWin {|x, y|
		winDims = [x, y];
		this.changed(\winDims, [x, y]);
	}

	moveWin {|x=0, y=0|
		winPos = [x, y];
		this.changed(\winPos, [x, y]);
	}

	endFullScreen {
		this.changed(\endFullScreen);
	}

	fullScreen {
		this.changed(\fullScreen);
	}

	close {
		this.changed(\close);
		^this
	}

	winCol_ {|color|
		winCol = color;
		this.changed(\winCol, winCol);
		^this
	}

	viewCol_ {|color|
		viewCol = color;
		this.changed(\viewCol, viewCol);
		^this
	}

	marginCol_ {|color|
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

ViewCentral {
	var <win, <marginView, <dims, <view, <winCol, <margins;
	var <marginCol, <usrViewBool, <viewCol, <winName, model;


	*new { arg win, model;
		^super.new.init(win, model)
	}

	init { arg aWin, aModel;

		win = aWin;
		model = aModel;
		model.addDependant(this);

		usrViewBool = model.usrViewBool;

		dims = model.dims; // these remain a fixed size

		if(win.isNil, { win = Window.new.front});

		// for when view is remade
		if(model.winPos.notNil){
			var pos = model.winPos;
			this.moveWin(pos[0], pos[1])
		};

		this.regsiterWinAction(win);

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

		this.marginCol_(model.marginCol);
		this.viewCol_(model.viewCol);
		this.winCol_(model.winCol);

		// kludge, win won't resize immediately,
		// maybe problem with layouts?
		r{
			0.0001.wait;
			this.resizeWin(*model.winDims);
		}.play(AppClock);

		^this
	}

	registerKeyDownAction {|escKey|
		win.view.keyDownAction_({
			arg view, char, modifiers, unicode, keycode, key;
			if(keycode==escKey, {win.endFullScreen});
		});
		^this
	}


	regsiterWinAction {|win|
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

		this.margins_(model.margins);
		win.layout.margins_(0); // window margins
		^this
	}

	resizeWin {|x, y|
		win.setInnerExtent(x, y);
		^this
	}

	moveWin {|x, y|
		var bounds, rect;
		bounds = win.bounds;
		rect = Rect(x, y, bounds.width, bounds.height);
		win.setTopLeftBounds(rect, menuSpacer: 45);
		^this
	}

	marginCol_ {|aCol|
		marginCol = aCol;
		marginView.background_(marginCol);
		^this
	}

	viewCol_ {|aCol|
		viewCol = aCol;
		view.background_(viewCol);
		^this
	}

	winCol_ {|aCol|
		winCol =  aCol;
		win.background_(winCol);
		^this
	}

	winName_{|aName|
		winName = aName;
		win.name(winName);
	}

	margins_ { |argMargins|
		margins = argMargins;
		marginView.layout.margins = margins; // LTRB
		^this
	}

	// remove all views from the window
	remove {
		if(win.isClosed.not, {
			view.remove;
			marginView.remove;
		});
		^this;
	}

	close { win.close; ^this }

	update {|obj, what, val|
		case{what == \margins} {
			this.margins_(val) // LTRB
		}
		{what == \winDims} {
			this.resizeWin(*val)
		}
		{what == \winPos} {
			this.moveWin(*val)
		}
		{what == \winCol} {
			this.winCol_(val)
		}
		{what == \viewCol} {
			this.viewCol_(val)
		}
		{what == \marginCol} {
			this.marginCol_(val)
		}
		{what == \winName} {
			this.winName_(val)
		}
		{what == \close} {
			this.close
		}
		{what == \keyDownAction} {
			this.registerKeyDownAction(val)
		}
		{what == \fullScreen} {
			win.fullScreen
		}
		{what == \endFullScreen} {
			win.endFullScreen
		};
		^this
	}
}

