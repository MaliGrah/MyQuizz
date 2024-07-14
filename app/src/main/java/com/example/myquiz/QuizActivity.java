package com.example.myquiz;

import android.content.Intent;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.example.myquiz.helpers.QuizDbHelper;
import com.example.myquiz.models.Question;

import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class QuizActivity extends AppCompatActivity {

    public static final String EXTRA_SCORE = "extraScore";

    private TextView textViewQuestion;
    private TextView textViewScore;
    private TextView textViewQuestionCount;
    private TextView textViewCategory;
    private TextView textViewDifficulty;
    private TextView textViewCountdown;
    private RadioGroup radioGroup;
    private RadioButton radioButton1;
    private RadioButton radioButton2;
    private RadioButton radioButton3;
    private Button buttonConfirmNext;

    private List<Question> questionList;
    private Question currentQuestion;
    private int questionCounter;
    private int questionCountTotal;
    private int score;
    private boolean answered;

    private CountDownTimer countDownTimer;
    private long timeLeftInMillis;
    private static final long COUNTDOWN_IN_MILLIS = 30000;

    private long backPressedTime; // Added variable to track back press time

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        textViewQuestion = findViewById(R.id.text_view_question);
        textViewScore = findViewById(R.id.text_view_score);
        textViewQuestionCount = findViewById(R.id.text_view_question_count);
        textViewCategory = findViewById(R.id.text_view_category);
        textViewDifficulty = findViewById(R.id.text_view_difficulty);
        textViewCountdown = findViewById(R.id.text_view_countdown);
        radioGroup = findViewById(R.id.radio_group);
        radioButton1 = findViewById(R.id.radio_button1);
        radioButton2 = findViewById(R.id.radio_button2);
        radioButton3 = findViewById(R.id.radio_button3);
        buttonConfirmNext = findViewById(R.id.button_confirm_next);

        Intent intent = getIntent();
        String categoryID = intent.getStringExtra(StartingScreenActivity.EXTRA_CATEGORY_ID);
        String categoryName = intent.getStringExtra(StartingScreenActivity.EXTRA_CATEGORY_NAME);
        String difficulty = intent.getStringExtra(StartingScreenActivity.EXTRA_DIFFICULTY);

        textViewCategory.setText("Category: " + categoryName);
        textViewDifficulty.setText("Difficulty: " + difficulty);

        QuizDbHelper dbHelper = QuizDbHelper.getInstance();
        dbHelper.getQuestionsForCategoryAndDifficulty(categoryID, difficulty, new QuizDbHelper.QuestionDataStatus() {
            @Override
            public void DataIsLoaded(List<Question> questions) {
                questionList = questions;
                questionCountTotal = questionList.size();
                Collections.shuffle(questionList);
                showNextQuestion();
            }

            @Override
            public void onError(String errorMessage) {
                // Handle error loading questions
            }
        });

        buttonConfirmNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!answered) {
                    if (radioButton1.isChecked() || radioButton2.isChecked() || radioButton3.isChecked()) {
                        checkAnswer();
                    } else {
                        Toast.makeText(QuizActivity.this, "Please select an answer", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    showNextQuestion();
                }
            }
        });
    }

    private void showNextQuestion() {
        radioGroup.clearCheck();

        // Reset text clr
        radioButton1.setTextColor(getResources().getColor(android.R.color.black));
        radioButton2.setTextColor(getResources().getColor(android.R.color.black));
        radioButton3.setTextColor(getResources().getColor(android.R.color.black));

        if (questionCounter < questionCountTotal) {
            currentQuestion = questionList.get(questionCounter);

            textViewQuestion.setText(currentQuestion.getQuestion());
            radioButton1.setText(currentQuestion.getOption1());
            radioButton2.setText(currentQuestion.getOption2());
            radioButton3.setText(currentQuestion.getOption3());

            questionCounter++;
            textViewQuestionCount.setText("Question: " + questionCounter + "/" + questionCountTotal);
            answered = false;
            buttonConfirmNext.setText("Confirm");

            timeLeftInMillis = COUNTDOWN_IN_MILLIS;
            startCountdown();
        } else {
            finishQuiz();
        }
    }


    private void startCountdown() {
        countDownTimer = new CountDownTimer(timeLeftInMillis, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                timeLeftInMillis = millisUntilFinished;
                updateCountdownText();
            }

            @Override
            public void onFinish() {
                timeLeftInMillis = 0;
                updateCountdownText();
                checkAnswer();
            }
        }.start();
    }

    private void updateCountdownText() {
        int minutes = (int) (timeLeftInMillis / 1000) / 60;
        int seconds = (int) (timeLeftInMillis / 1000) % 60;

        String timeFormatted = String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds);
        textViewCountdown.setText(timeFormatted);

        if (timeLeftInMillis < 10000) {
            textViewCountdown.setTextColor(getResources().getColor(android.R.color.holo_red_light));
        } else {
            textViewCountdown.setTextColor(getResources().getColor(android.R.color.black));
        }
    }

    private void checkAnswer() {
        answered = true;

        countDownTimer.cancel();

        RadioButton selectedRadioButton = findViewById(radioGroup.getCheckedRadioButtonId());
        int answerNr = radioGroup.indexOfChild(selectedRadioButton) + 1;

        int correctAnswerNr = Integer.parseInt(currentQuestion.getAnswerNr());

        if (answerNr == correctAnswerNr) {
            score++;
            textViewScore.setText("Score: " + score);
        }

        showSolution();
    }

    private void showSolution() {
        radioButton1.setTextColor(getResources().getColor(android.R.color.black));
        radioButton2.setTextColor(getResources().getColor(android.R.color.black));
        radioButton3.setTextColor(getResources().getColor(android.R.color.black));

        switch (Integer.parseInt(currentQuestion.getAnswerNr())) {
            case 1:
                radioButton1.setTextColor(getResources().getColor(android.R.color.holo_green_light));
                break;
            case 2:
                radioButton2.setTextColor(getResources().getColor(android.R.color.holo_green_light));
                break;
            case 3:
                radioButton3.setTextColor(getResources().getColor(android.R.color.holo_green_light));
                break;
        }

        if (questionCounter < questionCountTotal) {
            buttonConfirmNext.setText("Next");
        } else {
            buttonConfirmNext.setText("Finish");
        }
    }

    private void finishQuiz() {
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_SCORE, score);
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    public void onBackPressed() {
        if (backPressedTime + 2000 > System.currentTimeMillis()) {
            finishQuiz();
        } else {
            Toast.makeText(this, "Press back again to finish", Toast.LENGTH_SHORT).show();
        }

        backPressedTime = System.currentTimeMillis();
    }
}
