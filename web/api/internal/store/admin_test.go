package store

import (
	"slices"
	"strings"
	"testing"
	"time"
)

func TestTheUIComponentsFilterIsBoundAsAParameter(t *testing.T) {
	day := time.Date(2026, 10, 25, 0, 0, 0, 0, time.UTC)
	hostile := "with' OR '1'='1"
	var p params
	from, where := Filter{From: day, To: day.AddDate(0, 0, 1), UIComponents: hostile}.generations(&p)
	if strings.Contains(from+where, hostile) {
		t.Fatalf("the value reached the SQL: %s WHERE %s", from, where)
	}
	if !strings.Contains(where, "("+uiComponents+") = $3") || len(p.args) != 3 || p.args[2] != hostile {
		t.Fatalf("WHERE %s, args %v", where, p.args)
	}

	p = params{}
	if _, where := (Filter{From: day, To: day.AddDate(0, 0, 1)}).generations(&p); strings.Contains(where, uiComponents) {
		t.Fatalf("an unset filter still narrows: %s", where)
	}
}

func TestTheDailyRollupIsNotReadUnderAFilterItDoesNotKeep(t *testing.T) {
	if !(Filter{Preset: "lean", Country: "IN"}).rollupCovers() {
		t.Fatal("the rollup keeps presets and countries")
	}
	for _, f := range []Filter{{Source: "hn"}, {UIComponents: "without"}} {
		if f.rollupCovers() {
			t.Fatalf("the rollup has no column for %+v", f)
		}
	}
}

func TestWeeklyAdoptionCountsEveryDimension(t *testing.T) {
	if !slices.Contains(AdoptionDimensions, "ui_components") {
		t.Fatalf("dimensions %v", AdoptionDimensions)
	}
	for _, dim := range append([]string{"total", "features"}, AdoptionDimensions...) {
		if !strings.Contains(weeklyAdoption, "'"+dim+"'") {
			t.Errorf("the weekly query never counts %s", dim)
		}
	}
	if !strings.Contains(weeklyAdoption, uiComponents) {
		t.Fatal("the weekly query reads ui_components some other way than the filter does")
	}
	for _, value := range UIComponentsValues {
		if !strings.Contains(uiComponents, "'"+value+"'") {
			t.Errorf("the expression never yields %q", value)
		}
	}
}
