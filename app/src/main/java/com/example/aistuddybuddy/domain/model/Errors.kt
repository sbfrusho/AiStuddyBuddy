package com.example.aistuddybuddy.domain.model

class MissingApiKeyException(val provider: AiProvider) :
    Exception("Add your ${provider.displayName} API key in Settings to use it.")

class NoTextFoundException :
    Exception("No text found in the selected area. Try selecting the question more tightly or retake the photo with better light.")

class NoQuestionsFoundException :
    Exception("No questions found in the selected area. Try selecting the question more tightly.")
