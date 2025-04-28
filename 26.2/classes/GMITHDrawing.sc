
/*
© 2022 Tom Hall
*/



GMITHDrawing {
	var lineData, <dashPattern, <>fontSize;

	*new {|lineData|
		^super.new.init(lineData);
	}

	init {|argLineData|
		lineData = argLineData;
		fontSize = 12;
	}

	// draw the entire grid
	drawGrid {|width=1, color, patterns, dashPattern|
		var lineStyle, patternSize, singlePattern=false;
		color = if(color.isNil) {Color.black} {color};
		Pen.color_(color);
		Pen.width = width;
		patterns = if(patterns.isNil){
			[[1],[1]];
		}{
			patterns
		};

		lineData.do({|k, q|
			// [hLines, vLines]
			patternSize = patterns[q].size;
			if(patternSize==1) {
				lineStyle = this.prLineStyle(patterns[q][0], dashPattern);
				singlePattern = true;
			};
			k.do{|i, j|
				if(singlePattern.not){
					lineStyle = patterns[q].at(j.mod(patternSize));
				};
				if(lineStyle != 0){
					lineStyle = this.prLineStyle(lineStyle, dashPattern);
					Pen.line(i[0], i[1]);
					if(singlePattern.not){Pen.stroke};
				};
			};
			if(singlePattern){Pen.stroke};
			singlePattern = false;
		});
		Pen.lineDash_(FloatArray[1, 0]);
		^this
	}

	prLineStyle {|patternInt aDashPattern|
		dashPattern = aDashPattern;
		case {patternInt==0} {0}
		{patternInt==1} {Pen.lineDash_(FloatArray[1, 0])}
		{patternInt==1.neg}{Pen.lineDash_(dashPattern)};
		^patternInt
	}

	// prProcessCellText {|text|
	// 	var stack;
	// 	if(text.isString.not){
	// 		if(text.isArray){
	// 			text.do({|i|
	// 				if(stack.notNil){stack = stack++"\n"};
	// 				stack = stack++(i.asString)
	// 			});
	// 			text = stack;
	// 		} {
	// 			text = text.asString
	// 		};
	// 	};
	// 	^text
	// }

	drawScoreEvents  { |cells, matrix, penWidth, cellRangeOffset=0|
		var rect, pos, txt, draw, row, drawBool;

		drawBool = true;
		draw = GMITHEventShapes.new(penWidth, penWidth/2);
		draw.fontSize = fontSize;

		// iterate over selected segment of score
		cells.keysValuesDo{|key, value|
			pos = [key[0], key[1]];
			//process text
			txt = if((value[\txt]).notNil) {
				// prProcessCellText is done at earlier score to cell render stage
				// this.prProcessCellText(value[\txt])
				txt  = value[\txt].asString;
			}{
				nil
			};
			// will check row is also in range
			row = matrix.at(pos[0]);
			if(drawBool = row.notNil) {
				rect = row.at(pos[1]-cellRangeOffset)
			};

			if(drawBool, {
				// [pos, value[\type], txt, rect].postln;
				case
				{value[\type]==\plain} {
					draw.drawPlainCell(rect, txt);
				}
				{value[\type]==\square} {
					draw.drawSquare(rect, txt);
				}
				{value[\type]==\diamond} {
					draw.drawDiamond(rect, txt, value[\tail]);
				}
				{value[\type]==\squareDiamond} {
					draw.drawSquare(rect, txt, diamond: true);
				}
				{value[\type]==\diamondSml} {
					draw.drawDiamondSmall(rect, txt);
				}
				{value[\type]==\dotEnd} {
					draw.drawEndCellDotted(rect, txt);
				}
				{value[\type]==\start} {
					draw.drawStartCell(rect, txt);
				}
				{value[\type]==\startSquareDiamond} {
					draw.drawStartCell(rect, txt, diamond: true);
				}
				{value[\type]==\mid} {
					draw.drawMidCell(rect, txt);
				}
				{value[\type]==\dotMid} {
					draw.drawMidCell(rect, txt, true); // dotted
				}
				{value[\type]==\end} {
					draw.drawEndCell(rect, txt);
				}
				{true}{format("No type match at % for '%'", pos, value[\type]).error}
				;
			}, {
				format("cell event % is outside scope of grid dims [%, %]",
					key, matrix.size, matrix[0].size).warn;
			});
		};
	}

}


GMITHEventShapes {

	var <rectShrink, <>fontName, <>fontSize, <>fontColor;
	var <penWidth, <>diamondScale, <>color;

	*new {|penWidth, rectShrink|
		^super.new.init(penWidth, rectShrink);
	}

	init {|argPenWidth, argRectShrink|
		penWidth = argPenWidth;
		rectShrink = argRectShrink;
		diamondScale = [0.75, 0.925];
		color = Color.black;
		fontSize = 12;
		fontName = Font.defaultSerifFace;
		fontColor = Color.black;
	}

	font {
		^Font(fontName, fontSize);
	}

	dashPattern {|rect|
		var arr, sqSize = rect.width;
		arr = FloatArray[
			(sqSize/10) * penWidth.reciprocal,
			(sqSize/8) * penWidth.reciprocal
		];
		^arr
	}

	// this.drawText(rect, txt)
	drawText { arg aRect, aTxt, diamond = false;
		var newlines, aFont;
		newlines = aTxt.findAll("\n").size;
		// change font to accommodate text
		aFont = case{newlines>1}{
			this.font.copy.size_(fontSize * 0.7);
		}
		{aTxt.size>3 or:{diamond}}{
			this.font.copy.size_(fontSize * 0.9)
		}
		{true}{
			this.font
		};
		aTxt.drawCenteredIn(aRect, aFont, fontColor);
		^this

	}

	// plain cell (text only)
	drawPlainCell {|rect, txt|
		rect = rect.insetBy(rectShrink);
		if(txt.notNil, {this.drawText(rect, txt)});
	}

	// square cell
	drawSquare {|rect, txt, diamond = false|
		var diamondPts;
		rect = rect.insetBy(rectShrink);
		Pen.color_(color);
		if(txt.notNil, {this.drawText(rect, txt, diamond: diamond)});
		Pen.width = penWidth;
		Pen.capStyle_(1);
		Pen.joinStyle_(1);
		Pen.addRect(rect);
		Pen.stroke;

		// diamond
		if(diamond, {
			diamondPts = this.diamondPointData(
				rect, scaleX: diamondScale[0], scaleY: diamondScale[1]
			);
			Pen.moveTo(diamondPts[0]); // top
			diamondPts.rotate(-1).do{|i| Pen.lineTo(i)};
			Pen.width = penWidth * 0.65;
			Pen.stroke;
			Pen.width = penWidth;
		});
	}

	// diamond cell SMALL
	drawDiamondSmall { |rect, txt, tail=false|
		var diamondPts;
		rect = rect.insetBy(rectShrink);
		diamondPts = this.diamondPointData(
			rect, scaleX: diamondScale[0], scaleY: diamondScale[1]
		);
		Pen.color_(color);
		if(txt.notNil, {this.drawText(rect, txt, diamond: true)});
		Pen.width = penWidth * 0.65;
		Pen.capStyle_(1);
		Pen.joinStyle_(1);
		// diamond
		Pen.moveTo(diamondPts[0]); // top
		diamondPts.rotate(-1).do{|i| Pen.lineTo(i)};
		Pen.stroke;
		Pen.width = penWidth;
	}

	diamondPointData { |rect, scaleX = 1, scaleY =1|
		var top, right, bottom, left;
		top = (rect.left + (rect.width/2))@(rect.top + (rect.height * (1 - scaleY)));
		right = (rect.right - (rect.width * (1 - scaleX)))@ (rect.top + (rect.height/2));
		bottom =(rect.left + (rect.width/2))@(rect.bottom - (rect.height * (1 - scaleY)));
		left = (rect.left + (rect.width * (1 - scaleX)))@ (rect.top + (rect.height/2));
		^[top, right, bottom, left];
	}


	// start cell
	drawStartCell {|rect, txt, diamond = false|
		var diamondPts;
		rect = rect.insetAll(rectShrink, rectShrink, 0, rectShrink);
		Pen.color_(color);
		if(txt.notNil, {this.drawText(rect, txt, diamond: diamond)});
		Pen.width = penWidth;
		Pen.capStyle_(1);
		Pen.joinStyle_(1);
		Pen.moveTo(rect.rightTop);
		Pen.lineTo(rect.leftTop);
		Pen.lineTo(rect.leftBottom);
		Pen.lineTo(rect.rightBottom);
		Pen.stroke;

		if(diamond, {
			diamondPts = this.diamondPointData(
				rect, scaleX: diamondScale[0], scaleY: diamondScale[1]
			);
			Pen.moveTo(diamondPts[0]); // top
			diamondPts.rotate(-1).do{|i| Pen.lineTo(i)};
			Pen.width = penWidth * 0.65;
			Pen.stroke;
			Pen.width = penWidth;

		});
	}

	// mid cell
	drawMidCell {|rect, txt, dotted = false|
		rect = rect.insetAll(0, rectShrink, 0, rectShrink);
		Pen.color_(color);
		if(txt.notNil, {this.drawText(rect, txt)});
		Pen.width = penWidth;
		Pen.capStyle_(1);
		Pen.joinStyle_(1);
		if(dotted) {
			Pen.lineDash_(this.dashPattern(rect))
		};
		Pen.line(rect.leftTop, rect.rightTop);
		Pen.line(rect.leftBottom, rect.rightBottom);
		Pen.stroke;
		if(dotted, {Pen.lineDash_(FloatArray[1,0])}); // solid;
	}

	// end cell
	drawEndCell {|rect, txt|
		rect = rect.insetAll(0, rectShrink, rectShrink, rectShrink);
		Pen.color_(color);
		if(txt.notNil, {this.drawText(rect, txt)});
		Pen.width = penWidth;
		Pen.capStyle_(1);
		Pen.joinStyle_(1);
		Pen.moveTo(rect.leftTop);
		Pen.lineTo(rect.rightTop);
		Pen.lineTo(rect.rightBottom);
		Pen.lineTo(rect.leftBottom);
		Pen.stroke;
	}

	/*
	// No scaling
	diamondPointData { |rect|
	var top, right, bottom, left;
	top = rect.leftTop + rect.rightTop/2;
	right = rect.rightTop + rect.rightBottom/2;
	bottom = rect.leftBottom + rect.rightBottom/2;
	left = rect.leftTop + rect.leftBottom/2;
	^[top, right, bottom, left];
	}
	*/


	// diamond cell
	drawDiamond { |rect, txt, tail=false|
		var diamondPts;
		// ["rect": rect].postln;
		rect = rect.insetBy(rectShrink);
		// ["rectShrink": rect].postln;
		diamondPts = this.diamondPointData(rect, 1, 1);
		// ["diamondPts": diamondPts].postln;
		Pen.color_(color);
		if(txt.notNil, {this.drawText(rect, txt, diamond:true)});
		Pen.width = penWidth;
		Pen.capStyle_(1);
		Pen.joinStyle_(1);

		Pen.moveTo(diamondPts[0]);
		diamondPts.rotate(-1).do{|i| Pen.lineTo(i)};
		Pen.stroke;

		if(tail.notNil and:{tail}, {
			// optional dotted lines to right
			Pen.lineDash_(this.dashPattern(rect));
			Pen.moveTo(rect.leftTop + rect.rightTop/2);
			Pen.lineTo(rect.rightTop);
			Pen.moveTo(rect.leftBottom + rect.rightBottom/2);
			Pen.lineTo(rect.rightBottom);
			Pen.stroke;
			Pen.lineDash_(FloatArray[1,0]); // solid
		});
	}



	// end cell (dotted)
	drawEndCellDotted { |rect, txt|
		rect = rect.insetAll(0, rectShrink, rectShrink, rectShrink);
		Pen.color_(color);
		if(txt.notNil, {this.drawText(rect, txt)});
		Pen.width = penWidth;
		Pen.capStyle_(1);
		Pen.joinStyle_(1);
		Pen.lineDash_(this.dashPattern(rect));
		Pen.moveTo(rect.leftTop);
		Pen.lineTo(rect.rightTop);
		Pen.moveTo(rect.rightBottom);
		Pen.lineTo(rect.leftBottom);
		Pen.stroke;
		Pen.lineDash_(FloatArray[1,0]);
		Pen.moveTo(rect.rightTop);
		Pen.lineTo(rect.rightBottom);
		Pen.stroke;

	}
}

