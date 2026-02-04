package com.radio.ccbes.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.font.FontWeight
import com.radio.ccbes.ui.theme.RedAccent

class HashtagVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        return TransformedText(
            text = highlightHashtags(text.text),
            offsetMapping = OffsetMapping.Identity
        )
    }

    private fun highlightHashtags(text: String): AnnotatedString {
        val builder = AnnotatedString.Builder()
        val words = text.split(" ", "\n")
        
        var lastIndex = 0
        val regex = Regex("#\\w+")
        val matches = regex.findAll(text)
        
        for (match in matches) {
            // Append text before match
            builder.append(text.substring(lastIndex, match.range.first))
            
            // Append highlighted hashtag
            builder.withStyle(style = SpanStyle(color = RedAccent)) {
                append(match.value)
            }
            
            lastIndex = match.range.last + 1
        }
        
        // Append remaining text
        if (lastIndex < text.length) {
            builder.append(text.substring(lastIndex))
        }
        
        return builder.toAnnotatedString()
    }
}

/**
 * Utility function to highlight hashtags and URLs in any string and return an AnnotatedString.
 */
fun String.highlightPostContent(): AnnotatedString {
    val builder = AnnotatedString.Builder()
    val hashtagRegex = Regex("#\\w+")
    val urlRegex = Regex("(https?://[^\\s]+|www\\.[^\\s]+)")
    
    val allMatches = (hashtagRegex.findAll(this) + urlRegex.findAll(this))
        .sortedBy { it.range.first }
        .toList()
    
    var lastIndex = 0
    for (match in allMatches) {
        // Prevent overlapping or backwards indexing
        if (match.range.first < lastIndex) continue
        
        builder.append(this.substring(lastIndex, match.range.first))
        
        val isUrl = match.value.startsWith("http") || match.value.startsWith("www")
        val color = if (isUrl) Color(0xFF1D9BF0) else RedAccent // Twitter blue for links
        
        builder.pushStringAnnotation(tag = if (isUrl) "URL" else "HASHTAG", annotation = match.value)
        builder.withStyle(style = SpanStyle(color = color, fontWeight = if (isUrl) FontWeight.Normal else FontWeight.Bold)) {
            append(match.value)
        }
        builder.pop()
        
        lastIndex = match.range.last + 1
    }
    
    if (lastIndex < this.length) {
        builder.append(this.substring(lastIndex))
    }
    
    return builder.toAnnotatedString()
}

// Keep old function for compatibility or replace usages
fun String.highlightHashtags(): AnnotatedString = this.highlightPostContent()
