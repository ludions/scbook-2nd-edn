/*
RatioArray
Class used with ScreenRatios to maintain backwards compatability with the SuperCollider Boook, 2nd Edn, 2025

© 2025 Tom Hall
ludions.com

*/

RatioArray : Object {

    var <array;
    var <>description;

    *new { |array, description|
        ^super.new.init(array, description);
    }

    init { |argArray, argDescription|
        array = argArray;
        description = argDescription ? "";
        ^this;
    }

    // Array-like access
    at { |index|
        ^array[index];
    }

	ratio {
        ^array;
    }

    // Allow array indexing with []
    valueAt { |index|
        ^this.at(index);
    }

    // collection methods
    size { ^array.size }

    do { |func| array.do(func) }

    collect { |func| ^array.collect(func) }

    sum { ^array.sum }

    asArray { ^array }

    // string representation
    printOn { |stream|
        stream << this.class.name << "(" << array << " | \"" << description << "\")";
    }
}