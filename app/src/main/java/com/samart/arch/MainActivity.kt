package com.samart.arch

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
/*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.setContent
import androidx.compose.ui.tooling.preview.Preview
*/
import com.samart.arch.ui.theme.ArchDemoTheme
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch

/**
 * @see ExampleUnitTest
 */
class MainActivity : AppCompatActivity() {
	override fun onCreate(savedInstanceState: Bundle?) {
		super.onCreate(savedInstanceState)
/*
		setContent {
			ArchDemoTheme {
				// A surface container using the 'background' color from the theme
				Surface(color = MaterialTheme.colors.background) {
					Greeting("Android")
				}
			}
		}
*/
		MainScope().launch { hello() }
	}

	private fun hello() {
		Log.d("xoxoxo", "hello: xoxoxo")
	}
}

/*
@Composable
fun Greeting(name: String) {
	Text(text = "Hello $name!")
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
	ArchDemoTheme {
		Greeting("Android")
	}
}*/
