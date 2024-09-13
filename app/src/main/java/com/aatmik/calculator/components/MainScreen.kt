package com.aatmik.calculator.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.aatmik.calculator.Category.Category


@Composable
fun MainScreen(
    modifier: Modifier,
    buttonSpacing: Dp = 8.dp,
    category: (Category) -> Unit,
) {

    Box(modifier = modifier) {
        Column(
            Modifier.fillMaxSize()
        ) {
            Search()

            CategoryList()

            BasicCalculator()
        }

    }

}

@Composable
fun BasicCalculator() {
    /*
    * bottom of the screen
    * round button radius 50 dp card
    * calculator text
    * icon of calculator
    * floating button with elevation
    * */

    Text(text = "Calculator")
}

@Composable
fun CategoryList() {
    /*
    * grid of categories
    * 1 category -> square card box white
    * icon inside coloured
    * name of category bottom of card
    *
    * */

    Text(text = "Category")
}

@Composable
fun Search() {
    // box -> radius 5 dp ,white card
    // 2 icons -> search ic , search Text Field

    Text(text = "Search")

    // Use MaterialTheme colors and typography
    val cardColor = MaterialTheme.colorScheme.surface
    val textColor = MaterialTheme.colorScheme.onSurface

    // Create a white card with rounded corners and elevation
    Card(
        modifier = Modifier
            .padding(16.dp)
            .fillMaxWidth(),
        shape = MaterialTheme.shapes.medium
    ) {
        // Row containing search icon, search text field, and optional clear icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Search icon
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = textColor
            )

            // Search text field
            var searchText by remember { mutableStateOf("") }

            BasicTextField(
                value = searchText,
                onValueChange = { newText ->
                    searchText = newText
                },
                keyboardOptions = KeyboardOptions.Default.copy(
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(
                    onSearch = {
                        // Handle search action here
                    }
                ),
                // textStyle = MaterialTheme.typography.body1.copy(color = textColor),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 16.dp)
                    .fillMaxWidth()
            )
        }
    }
}
