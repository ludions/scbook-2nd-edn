
/*
ViewCentralOV
An optional overview facade/coordinator example for ViewCentral (view) and ViewCentralModel (model)

© 2025 Tom Hall

*/


ViewCentralOV {
	var <window, <model, <view, <>autoRecreate = false;

	*new { |dims, margins, usrViewBool, win|
		^super.new.init(dims, margins, usrViewBool, win);
	}

	init { |dims, margins, usrViewBool, win|

		model = ViewCentralModel.new(dims, margins, usrViewBool);

		// create the view with window
		this.recreateView(win);

		^this
	}

	// recreate view while maintaining model state
	recreateView { |win|
		// Clean up old view if it exists
		if(view.notNil and: { view.win.isClosed.not }) {
			view.win.close;
		};

		// create a new window if none provided
		win = win ?? { Window().front };

		// create new view with existing model
		view = ViewCentral.new(win, model);

		window = win;

		^this
	}

	// check if view exists and is open
	isViewClosed {
		^view.isNil or: { view.win.isClosed };
	}

	// ensure view exists, recreating if needed
	ensureViewExists {
		if(this.isViewClosed) {
			"Recreating closed view.".postln;
			this.recreateView;
		};
		^this
	}

	// access to content ie ViewCentral's view
	contentView {
		this.ensureViewExists;
		^view.view;
	}

	// refresh ViewCentral's view
	refresh {
		this.ensureViewExists;
		view.view.refresh;
		^this
	}

	// forward method calls to model
	doesNotUnderstand { |selector...args|
		// first check if there is a valid view
		if(this.isViewClosed) {
			if(autoRecreate) {
				"Automatically recreating closed view.".postln;
				this.recreateView;
			}{
				"View is closed. Call recreateView() first.".error;
				^this
			}
		};
		// try to forward to model
		if(model.respondsTo(selector)) {
			^model.performList(selector, args);
		};
		// try the view if model doesn't repsond to method
		if(view.respondsTo(selector)) {
			^view.performList(selector, args);
		};
		// neither model nor view understands the message
		^super.doesNotUnderstand(selector, *args);
	}

	// if ViewCentalOV is used as a container class, notify updates
	update { |obj, what, value|
		if(obj === model) {
			// forward all notifications
			this.changed(what, value);
		}
	}
}
