// Class that represents a question

package com.burningflower.pokemonquiz.model;

import android.os.Parcel;
import android.os.Parcelable;

public class Question {
    private final String question;
    private final String answer1;
    private final String answer2;
    private final String answer3;
    private final String answer4;
    private final String image;
    private final boolean specialImage;
    private final String audio;

    public Question (String question, String answer1, String answer2,
                     String answer3, String answer4, String image, boolean specialImage, String audio) {
        this.question = question;
        this.answer1 = answer1;
        this.answer2 = answer2;
        this.answer3 = answer3;
        this.answer4 = answer4;
        this.image = image;
        this.specialImage = specialImage;
        this.audio = audio;
    }

    public String getQuestion() {
        return question;
    }

    public String getAnswer1() {
        return answer1;
    }

    public String getAnswer2() {
        return answer2;
    }

    public String getAnswer3() {
        return answer3;
    }

    public String getAnswer4() { return answer4; }

    public String getImage () { return image; }

    public boolean isSpecialImage () { return specialImage; }

    public String getAudio () { return audio; }
}
