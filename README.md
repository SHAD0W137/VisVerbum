# VisVerbum - Android Word Learning Assistant

VisVerbum is an Android application designed to simplify the process of learning new words. It allows you to quickly get word definitions, save them for later study, and test your knowledge.

## Key Features

*   **Instant Definitions:** Highlight a word in any app, and VisVerbum (via a floating button and Accessibility Service) will help you find its meaning. Definitions are fetched from [DictionaryAPI](https://dictionaryapi.dev/).
*   **Personal Dictionary:** Save interesting or challenging words to your personal list, linked to your Firebase account.
*   **History & Review:** Easily browse all your saved words and delete those you've mastered.
*   **Knowledge Testing:** Quiz yourself by viewing random words from your saved list and recalling their meanings before checking the definition.
*   **User Settings:**
    *   Choose the language for a_word_definitions_received.
    *   Configure automatic word saving.
    *   Manage the application's display language.
*   **Security & Personalization:** Sign in with Email/Password or your Google account using Firebase Authentication to keep your data secure.

## Technologies Used

*   **Android (Java)**
*   **Firebase:** Authentication (Email/Password, Google), Realtime Database
*   **Android Services:** Foreground Service (for the floating button), Accessibility Service (for text capture)
*   **WindowManager API:** For displaying the definition window поверх other apps
*   **Third-party API:** [DictionaryAPI](https://dictionaryapi.dev/) for dictionary data

## Getting Started

1.  **Install the application.**
2.  **Sign up or log in** using your Email
3.  **Activate the main functionality** from the home screen:
    *   Grant permission to **"Display over other apps."**
    *   Enable **"VisVerbum Service"** in your device's **Accessibility settings.**
4.  Once activated, a floating button will appear. Highlight text in any app and tap the button (or use the "Define" option in the app's menu, if active) to get the definition.

---

*VisVerbum aims to make language learning more accessible and integrated into your daily smartphone experience.*
