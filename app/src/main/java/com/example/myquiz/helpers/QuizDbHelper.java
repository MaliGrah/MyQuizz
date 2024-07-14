package com.example.myquiz.helpers;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.myquiz.models.Category;
import com.example.myquiz.models.Question;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

public class QuizDbHelper {
    private static final String TAG = "QuizDbHelper";
    private static QuizDbHelper instance;
    private final DatabaseReference mDatabase;

    private QuizDbHelper() {
        mDatabase = FirebaseDatabase.getInstance("https://myquiz-24c3a-default-rtdb.europe-west1.firebasedatabase.app").getReference();
    }

    public static synchronized QuizDbHelper getInstance() {
        if (instance == null) {
            instance = new QuizDbHelper();
        }
        return instance;
    }

    public void getAllCategories(final CategoryDataStatus dataStatus) {
        mDatabase.child("categories").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Category> categories = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Log.d(TAG, "Category Snapshot: " + snapshot.toString());
                    try {
                        Category category = snapshot.getValue(Category.class);
                        if (category != null) {
                            category.setId(snapshot.getKey());  // Set the ID from the key
                            categories.add(category);
                            Log.d(TAG, "Category added: " + category.getName());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error converting value to Category", e);
                    }
                }
                dataStatus.DataIsLoaded(categories);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error fetching categories", databaseError.toException());
                dataStatus.onError(databaseError.getMessage());
            }
        });
    }

    public void getQuestionsForCategoryAndDifficulty(String categoryID, String difficulty, final QuestionDataStatus dataStatus) {
        mDatabase.child("questions").orderByChild("categoryID").equalTo(categoryID).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                List<Question> questions = new ArrayList<>();
                for (DataSnapshot snapshot : dataSnapshot.getChildren()) {
                    Log.d(TAG, "Question Snapshot: " + snapshot.toString());
                    try {
                        Question question = snapshot.getValue(Question.class);
                        if (question != null && question.getDifficulty().equals(difficulty)) {
                            // Ensure answerNr is fetched as String to avoid Firebase parsing issue
                            String answerNrString = snapshot.child("answerNr").getValue(String.class);
                            question.setAnswerNr(answerNrString);
                            questions.add(question);
                            Log.d(TAG, "Question added: " + question.getQuestion());
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "Error converting value to Question", e);
                    }
                }
                dataStatus.DataIsLoaded(questions);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) {
                Log.e(TAG, "Error fetching questions", databaseError.toException());
                dataStatus.onError(databaseError.getMessage());
            }
        });
    }

    public interface CategoryDataStatus {
        void DataIsLoaded(List<Category> categories);
        void onError(String errorMessage);
    }

    public interface QuestionDataStatus {
        void DataIsLoaded(List<Question> questions);
        void onError(String errorMessage);
    }
}
