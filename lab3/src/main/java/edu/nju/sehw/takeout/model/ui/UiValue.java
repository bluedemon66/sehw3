package edu.nju.sehw.takeout.model.ui;

/** Defines the text, exact decimal, and boolean field types exchanged with a UI. */
public sealed interface UiValue permits TextValue, DecimalValue, BooleanValue {}
