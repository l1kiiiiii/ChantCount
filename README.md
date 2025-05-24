
# Mantra Counter - Speech Recognition App

Mantra Counter is an Android application that helps users track their mantra repetitions using speech recognition. The app listens for a user-defined target sentence (mantra) and counts how many times it is spoken. When a predefined limit of repetitions is reached, an alarm sounds.

## Features

*   **Speech Recognition:** Utilizes Android's built-in speech recognition to listen for spoken words.
*   **Custom Target Sentence:** Users can type in any sentence or phrase they want to track.
*   **Predefined Mantra List:** Includes a spinner with a list of common mantras for quick selection.
*   **Repetition Limit:** Users can set a limit for the number of times the target sentence should be repeated.
*   **Match Counter:** Displays the current count of successful repetitions.
*   **Live Transcription (Partial Results):** Shows the recognized speech in real-time.
*   **Alarm Notification:** Plays a sound when the repetition limit is reached.
*   **Continuous Listening Mode:** Automatically restarts listening after a short delay to allow for continuous chanting/speaking.
*   **Permission Handling:** Properly requests and handles the necessary microphone permission.

## How It Works

1.  **Set Target Sentence:**
    *   The user can type their desired mantra/sentence into the "Target Sentence" input field.
    *   Alternatively, they can select a predefined mantra from the dropdown spinner.
2.  **Set Match Limit:**
    *   The user inputs a numerical value in the "Match Limit" field, defining how many times the target sentence should be spoken to trigger the alarm.
3.  **Start Listening:**
    *   Pressing the "START" button initiates the speech recognition. The button text changes to "STOP".
4.  **Speech Processing:**
    *   The app continuously listens for speech.
    *   It compares the recognized speech (converted to lowercase) against the target sentence (also converted to lowercase).
    *   The recognized text is displayed, with partial results updating live.
5.  **Counting Matches:**
    *   Each time the complete target sentence is detected within the stream of recognized words, the "Match Count" is incremented.
6.  **Reaching the Limit:**
    *   When the "Match Count" equals the "Match Limit":
        *   An alarm tone is played.
        *   A dialog box appears, notifying the user that the limit has been reached.
        *   After dismissing the dialog, the count is reset, and the app automatically restarts listening for the next cycle (if the user wishes to continue).
7.  **Stop Listening:**
    *   Pressing the "STOP" button halts the speech recognition process.

## Core Components Used

*   **`SpeechRecognizer`:** For converting spoken audio to text.
*   **`RecognitionListener`:** To handle events from the `SpeechRecognizer` (e.g., partial results, final results, errors).
*   **`EditText`:** For user input of the target sentence and match limit.
*   **`TextView`:** To display the recognized text and match count.
*   **`Button`:** To start and stop the listening process.
*   **`Spinner`:** To select from a predefined list of mantras.
*   **`ToneGenerator`:** To play an audible alarm.
*   **`AlertDialog`:** To notify the user when the match limit is reached.
*   **`Handler`:** To implement delays for continuous listening.
*   **Permissions:** `RECORD_AUDIO` permission is requested at runtime.

## Setup and Installation

1.  Clone the repository (or import the project into Android Studio).
2.  Build the project using Android Studio.
3.  Run the application on an Android device or emulator (Android API level will depend on the project's `minSdkVersion`).
4.  Grant microphone permission when prompted.

## Future Enhancements (Possible Ideas)

*   Save user preferences (last used mantra, limit).
*   More robust error handling and recovery for speech recognition.
*   Customizable alarm sounds.
*   Session history or logging.
*   Support for multiple languages in speech recognition.
*   Background listening mode (with appropriate user notifications and battery considerations).
