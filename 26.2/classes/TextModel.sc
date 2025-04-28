
/*
TextModel
© 2022 Tom Hall
version 2022-02
*/

GuiModel {

	var <background, <fixedHeight, <fixedWidth;
	var <maxWidth, <minWidth, <maxHeight, <minHeight;

	*new {
		^super.new.init
	}

	init {
		background = Color.grey(0.95);
		this.background_(background);
	}

	fixedHeight_ {|float|
		fixedHeight = float;
		if(minHeight.notNil){
			minHeight = nil;
			this.changed(\minHeight_, minHeight);
		};
		if(maxHeight.notNil){
			maxHeight = nil;
			this.changed(\maxHeight_, maxHeight);
		};
		this.changed(\fixedHeight_, fixedHeight);
		^this
	}

	minHeight_ {|float|
		minHeight = float;
		if(fixedHeight.notNil){
			fixedHeight = nil;
			this.changed(\fixedHeight_, fixedHeight);
		};
		this.changed(\minHeight_, minHeight);
		^this
	}

	maxHeight_ {|float|
		maxHeight = float;
		if(fixedHeight.notNil){
			fixedHeight = nil;
			this.changed(\fixedHeight_, fixedHeight);
		};
		this.changed(\maxHeight_, maxHeight);
		^this
	}

	fixedWidth_ {|float|
		fixedWidth = float;
		if(minWidth.notNil){
			minWidth = nil;
			this.changed(\minWidth_, minWidth);
		};
		if(maxWidth.notNil){
			maxWidth = nil;
			this.changed(\maxWidth_, maxWidth);
		};
		this.changed(\fixedWidth_, fixedWidth);
		^this
	}

	minWidth_ {|float|
		minWidth = float;
		if(fixedWidth.notNil){
			fixedWidth = nil;
			this.changed(\fixedWidth_, fixedWidth);
		};
		this.changed(\minWidth_, minWidth);
		^this
	}

	maxWidth_ {|float|
		maxWidth = float;
		if(fixedWidth.notNil){
			fixedWidth = nil;
			this.changed(\fixedWidth_, fixedWidth);
		};

		this.changed(\maxWidth_, maxWidth);
		^this
	}

	background_ {|color|
		background = color;
		this.changed(\background_, background);
		^this
	}

}

TextModel : GuiModel {

	var <align, <string, <stringColor, <fontSize;
	var <fontFace, <fontWeightBool, <fontStyleBool, <color;

	init {
		super.init;
		this.fontSize_(12);
		this.fontWeightBool_(false);
		this.fontStyleBool_(false);
		this.fontFace_(Font.defaultSerifFace);
		this.stringColor_(Color.black);
		this.align_(\center);
		this.string_("");
	}

	stringColor_ {|color|
		stringColor = color;
		this.changed(\stringColor_, stringColor);
		^this
	}

	color_ {|color|
		^this.stringColor_(color)
	}

	font {
		^Font(fontFace, fontSize, fontWeightBool, fontStyleBool);
	}

	font_{|fontFace, fontSize, fontWeightBool, fontStyleBool|
		if(fontFace.notNil){this.fontFace_(fontFace)};
		if(fontSize.notNil){this.fontSize_(fontSize)};
		if(fontWeightBool.notNil){this.fontWeightBool_(fontWeightBool)};
		if(fontStyleBool.notNil){this.fontStyleBool_(fontStyleBool)};
		^this
	}

	string_ { arg aString = "";
		string = aString;
		this.changed(\string_, string);
		^this
	}

	align_ { arg aSymbol;
		align = aSymbol;
		this.changed(\align_, align);
		^this
	}

	fontSize_ { |float|
		fontSize = float;
		this.changed(\font_, this.font);
		^this
	}

	fontFace_ { |face|
		fontFace = face;
		this.changed(\font_, this.font);
		^this
	}

	fontWeightBool_ { |bool|
		fontWeightBool = bool;
		this.changed(\font_, this.font);
		^this
	}

	fontStyleBool_ { |bool|
		fontStyleBool = bool;
		this.changed(\font_, this.font);
		^this
	}

}


