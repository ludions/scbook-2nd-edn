/*
Code compiled for The SuperCollider Book, 2nd Edn, MIT Press

26 Notations and Score-making

Tom Hall, Newton Armstrong, Richard Hoadley

CODE LISTING for section: ‘26.2 CPWN and graphic notation within SuperCollider’ only.

Full code listing for the complete chapter is available here:
"https://github.com/supercollider/scbookcode/tree/newedition/Ch 26 Notations and Score-making"

*/

// 26.2 CPWN and graphic notation within SuperCollider

// Class code at https://github.com/ludions

// 26.2.2		Page, frame, view

MITHScreenRatios().ratioToDims(\usLetter)

[sqrt(2): 1]

m = MITHScreenRatios(); m.viewDims_(m.ratioToDims(\usLetter)); MITHRatiosView(m);
m.viewDims_(m.ratioToDims(\iso, landscape: false));

// Figure 26.1: WinBlock used with MITHRatios

// dimensions for an iso ratio landscape Window, width 800px
~dims = MITHScreenRatios().ratioToDims(\iso, 800) // default is m.screenDims


// make a WinBlock, args: win, dims, margins, usrViewBool
b = WinBlock.new(dims: ~dims); b.moveWin(0, 0)

// view is by default an UserView
(
b.marginCol_(Color.white);
b.view.drawFunc = {|self|
	Pen.addRect(Rect(0.5, 0.5, self.bounds.width-1, self.bounds.height-1));
	Pen.stroke;
};
b.view.refresh;
)

b.viewDims // return current block dimensions

// experiment with blockSize / ratio
g = b.gui

// shape view dimensions
d = MITHScreenRatios().ratioToDims(\r5_3, ~dims * (5/6));
b.viewDims_(*d); b.centerView;

// show area revealed in fullscreen mode
b.winCol_(Color.new255(255, 85, 0)); b.resizeWin(*(~dims * 1.2));

// timed fullScreen, also ESC key (MBP) to endFullScreen
r{ b.fullScreen; 3.wait; b.endFullScreen}.play(AppClock)

b.shrinkWin // hide area beyond main margin
g.close; b.close;



// 26.2.3		Unicode and common practice Western notations

"🎹".asCodePoint // U+1F3B9
"U+1F3B9".asGlyph // 🎹

(
w = Window.new.front;
a = StaticText(w, Rect(100, 50, 300, 300));
a.font = Font("Bravura", 72);
a.string = "U+E050".asGlyph
)

a.string = ["U+1F1FA", "U+1F1E6"].scramble.asUnicodeString


// Figure 26.2: MITHNoteViewer dynamic note array presentation with Synth.

(
s.waitForBoot({
	SynthDef(\percSine, {|midi = 69, mul = 0.2, out=0|
		var sig, env = Env.perc(releaseTime:0.4);
		sig = SinOsc.ar(midi.midicps, mul: mul)
		    * EnvGen.kr(env, doneAction: Done.freeSelf);
		Out.ar(out, [sig, sig])
	}).add;

	l = MITHNoteViewer.new(8, 350);
	a = [60, 62, -63, 65, 67, 69, 71, 72, -70, -68, 67, 65, -63, 62, 60];
	r = Routine{
		l.fullScreen;
		1.wait;
		a.size.do{|i|
			Synth(\percSine, [\midi, a[i].abs]);
			l.add(a[i], slot: i.fold(0, 7), color:Color.red);
			if(p.notNil){l.glyphsCol_(Color.black, p)};
			p = i.fold(0, 7);
			(60/94).wait
		};
		l.glyphsCol_(Color.black, p);
		p = nil;
		l.endFullScreen;
		2.wait;
		l.clearAll;
		l.close
	};
	AppClock.play(r)
})
)


// 26.2.4 Model, grid, score

// Figure 26.4: Simple GMITH score creation and galley view display.

(
l = GMITH.new;
// [row, column] -> [number, type, duration]
l.addAll([
	[1, 0],
	[2, 1] -> [3],
	[0, 2] -> [nil, \h],
	[1, 3] -> ["Pz 3", nil, 2],
	[2, 5] -> [[2, 1]],
	[0, 6] -> [nil, \a]

]);
)

// |key, rows, cols, vHeightPct, win|
w = l.makeGalley(\gv1, cols: 8)

// to complete the score, click the bottom right (empty) cell, then evaluate:
l.addMouseCell([nil, \p]); // l.undo if in error

// post sorted score events l.scoreArr
l.postEvents(order: 0) // row order. order: 1 posts column order

// grid line patterns
l.gvGridPatterns(\gv1) // [ [ 1 ], [ 1 ] ]
l.gvGridPatterns_(\gv1, [1, 0, 0], [-1, 0, 0, 0]);

// display adjustments
l.gvColRange_(\gv1, 0, 63);
l.gvGridLineWidth(\gv1)
l.gvShapeLnWidth_(\gv1, 2) // adjust, default is 3
l.gvFontSizeScale_(\gv1, 0.8) // adjust
l.moveAll(12); // move score events to columns to the right
l.moveAll // return to 0 column as first event
l.gvColRange_(\gv1); // default view range is filled cell range



// Code example for image shown in Figure 26.5: Example of GMITH score view using makePage method

// Evaluate initial code in previous example first
l.class == GMITH // checking: needs to be true

// arg key, numSystems, dims, titleBool = false, win;
l.makePage(\title, 1, MITHScreenRatios().ratioToDims(\iso, 1000), true)

// initial placement, will be adjusted
// arg key, system, startCol, endCol, numRows = 3;
l.pageDrawSystem(\title, 0, 0, 7, 3);

// Adjust margins
g = l.pageMarginsGui(\title); // g.close if needed
l.pageViewDims(\title) // as pixels
// finalise as
l.pageViewDimsPct_(\title, 0.75, 0.8); // %s

// Enter page text
(
m = l.pageViewModel(\title); // access model
m.title.string_("a composition"); // bold by default
m.subtitle.string_("a subtitle");
m.composer.string_("a composer");
[m.title, m.subtitle, m.composer].do{|i| i.font_(Font.defaultSansFace, 16)};
m.midGutter.string_("Copyright 2022 some body");
m.midGutter.font_(Font.defaultSansFace, 12);
m.systemTopText(0).string = "a performance instruction";
m.systemTopText(0).font_(Font.defaultSansFace, 14, true);
// system text access can also be via a function see NotnFlow method sysTxtFnEval
m.systemInstrs_("instr", font:Font(Font.defaultSansFace, 14));
)

(
// finalise page elements layout
m.setSystemWidths(675);
m.setSystemNotnsHeight(254);
m.setSystemTopsHeight(50);
m.setGuttersHeight(125);// maxHeight
l.pageSysSqSize_(\title, 84.2); // Adjust notation size to fit
)

(
// Adjust notation defaults
l.pageGridPatterns_(\title, [1, 0, 0], [-1, 0]);
l.pageGridLnWdth_(\title, 1.5);
l.pageShapeLnWidth_(\title, 2.5);
l.pageFontSizeScale_(\title, 0.75);
)
// colors
// inner block background color
l.pageViewCol_(\title, Color.yellow)
// inner margins incl. between Layout Spacing
l.pageMarginCol_(\title, Color.rand)
// changes ALL backgrounds
l.pageColor_(\title, Color.white);
l.pageWinCol_(\title, Color.gray); // access outer margin color
l.pagefullScreen(\title); // ESC key to undo or use pageEndFullScreen


/////////////////////////////////////////////////////////////////////////////////

// Section 26.2 CODE LISTING END
