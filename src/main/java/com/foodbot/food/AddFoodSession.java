package com.foodbot.food;

public class AddFoodSession {
    public enum Step { AWAITING_NAME, AWAITING_PREP_TIME, AWAITING_INGREDIENTS }

    private Step step = Step.AWAITING_NAME;
    private String name;
    private int prepTimeMinutes;

    public Step getStep() {
        return step;
    }

    public void setStep(Step step) {
        this.step = step;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getPrepTimeMinutes() {
        return prepTimeMinutes;
    }

    public void setPrepTimeMinutes(int prepTimeMinutes) {
        this.prepTimeMinutes = prepTimeMinutes;
    }
}
