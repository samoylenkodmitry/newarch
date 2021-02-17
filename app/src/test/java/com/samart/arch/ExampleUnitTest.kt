package com.samart.arch

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.shareIn
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
	@Test
	fun coroutines() {

		runBlocking {
			method()
		}

	}

	@Test
	fun flowtest() {
		val f = flow<String> {
			emit("cache")
			emit("server")
		}

		runBlocking {
			f.collect { print(it) }
		}

	}

	private suspend fun method() {
		delay(100)
		print("hello")
	}

	sealed class ApiAnswer<T> {
		sealed class ValidAnswer<T>(open val value: T) : ApiAnswer<T>() {
			data class ServerAnswer<T>(override val value: T) : ValidAnswer<T>(value)
			data class CachedAnswer<T>(override val value: T) : ValidAnswer<T>(value)
		}

		data class ServerError<T>(val name: String) : ApiAnswer<T>()
	}


	data class User(val name: String = "user")
	data class Data(val num: Int = 0)

	class DataApi {
		fun getMyData(): Flow<ApiAnswer<Data>> {
			println("getMyData " + Thread.currentThread().name)
			return flow {
				println("getMyData flow " + Thread.currentThread().name)
				emit(ApiAnswer.ValidAnswer.CachedAnswer(Data(22)))
				delay(40)
				emit(ApiAnswer.ValidAnswer.ServerAnswer(Data(44)))
			}
		}
	}

	class UserApi {
		fun getMyUser(): Flow<ApiAnswer<User>> {
			log("getMyUser")
			return flow {
				log("emit cached user")
				emit(ApiAnswer.ValidAnswer.CachedAnswer(User("one")))
				log("wait")
				delay(100)
				log("emit server user")
				emit(ApiAnswer.ValidAnswer.ServerAnswer(User("two")))
			}
		}
	}

	class WidgetData(val user: User, val data: Data)
	class WidgetInteractor(private val dataApi: DataApi, private val userApi: UserApi) {

		fun getWidgetData(): Flow<WidgetData> {
			println("getWidgetData flow " + Thread.currentThread().name)
			return combine(
				dataApi.getMyData().filterErrors(),
				userApi.getMyUser().filterErrors(),
				{ data, user -> WidgetData(user.value, data.value) }
			)
		}
	}

	data class WidgetUiState(val number: Int, val text: String) : UiState()

	class WidgetUi {
		fun render(uiState: WidgetUiState) {
			log(
				"Screen:Widget ${uiState.number} ${uiState.text}"
			)
		}
	}

	class WidgetPresenter(private val ui: WidgetUi, private val widgetInteractor: WidgetInteractor) {
		val state = WidgetUiState(0, "init")
		suspend fun begin() {
			withContext(newSingleThreadContext("Ui thread")) {
				ui.render(state)
			}
			delay(200)
			log("begin begin")
			widgetInteractor
				.getWidgetData().collect { widgetData ->
					val uiState = WidgetUiState(
						widgetData.data.num,
						widgetData.user.name + " " +
							"\n" + widgetData.data.javaClass + " " +
							"\n" + widgetData.user.javaClass
					)
					if (uiState != state) {
						withContext(newSingleThreadContext("Ui thread")) {
							ui.render(uiState)
						}
					}
				}
			log("end begin")
		}
	}

	class Screen {
		private val widgetBackend = WidgetPresenter(WidgetUi(), WidgetInteractor(DataApi(), UserApi()))

		private var widgetJob: Job? = null

		fun start() {
			widgetJob = CoroutineScope(Dispatchers.IO).launch {
				widgetBackend.begin()
			}
		}

		fun stop() {
			widgetJob?.cancel()
		}
	}

	@Test
	fun api() {
		runBlocking {
			val mainScope = this
			log("begin")
			val shareUser = UserApi()
				.getMyUser()
				.filterErrors()
				.onEach { answer ->
					log("hello $answer")
				}
				.flowOn(Dispatchers.IO)
				.shareIn(mainScope, SharingStarted.Lazily)

			val shareData = DataApi()
				.getMyData()
				.filterErrors()
				.onEach { answer ->
					log("hello $answer")
				}
				.flowOn(Dispatchers.IO)
				.shareIn(mainScope, SharingStarted.Lazily)

			log("collect")

			log("in coroutine")
			combine(shareUser, shareData, { a, b -> "$a-$b" })
				.onEach {
					mainScope.launch {
						log("hello ui $it")
					}
				}
				.launchIn(CoroutineScope(Dispatchers.Default))
			delay(1000)
			log("end")
		}
	}

	open class UiState
	data class HeaderUiState(val text: String) : UiState()
	data class NumUiState(val text: String) : UiState()

	@Test
	fun sharedState() {
		val f = flow<String> {
			emit("hello")
		}

		val x = f
			.onEach { print("hi $it") }
			.shareIn(CoroutineScope(Dispatchers.IO), SharingStarted.Lazily)

		runBlocking {
			f.collect { print(it) }
			//show()
			x.collect { print(it) }
			//pause()
			//rotate()
			//resume()
			//------------screen
			x.collect { print("render UI - $it") }
		}


	}

	@Test
	fun another() {
		runBlocking {
			val mainScope = this
			//--------------------repository
			log("presenter:")
			val shareData = DataApi()
				.getMyData()
				//---------------presenter
				.filterErrors()
				.onEach { answer ->
					log("ooo $answer")
				}
				.map { NumUiState("-${it.value.num}-") }
				//-------off presenter
				.flowOn(Dispatchers.IO)
				.stateIn(mainScope)

			val widgetState = MutableStateFlow(WidgetUiState(3, ""))
			val headerState = MutableStateFlow(HeaderUiState("head"))

			//Presenter
			//fireState(...)
			widgetState.emit(WidgetUiState(3, ""))
			//Screen

			val m = mutableMapOf<Class<out UiState>, StateFlow<UiState>>()

			m[WidgetUiState::class.java] = widgetState
			m[HeaderUiState::class.java] = headerState
			m[NumUiState::class.java] = shareData

			//-----------screen
			delay(1000)
			log("screen:")

			m[NumUiState::class.java]!!.collect { print("ui render state num ") }

			m.forEach { cls, st ->

/*
				CoroutineScope(Dispatchers.Default).launch {
					(st as StateFlow<>)
					widgetState.collect { log("hello $it") }
				}
*/
			}
			CoroutineScope(Dispatchers.Default).launch {
				widgetState.collect { log("hello $it") }
			}
			CoroutineScope(Dispatchers.Default).launch {
				headerState.collect { log("hi $it") }
			}
			CoroutineScope(Dispatchers.Default).launch {
				shareData.collect { log("bonjur $it") }
			}
		}
	}

	@Test
	fun archLifecycle() {
		println("-----------------------")
		println("hello " + Thread.currentThread().name)
		val s = Screen()
		log("start screen")
		s.start()
		Thread.sleep(400)
		log("stop screen")
		s.stop()
		Thread.sleep(400)
		log("end")
		println("-----------------------")
	}

	companion object {
		@JvmStatic
		fun log(mes: String) {
			println(mes + " " + Thread.currentThread().id)
		}
	}
}

fun <T> Flow<ExampleUnitTest.ApiAnswer<T>>.filterErrors() = this.flatMapConcat {
	if (it is ExampleUnitTest.ApiAnswer.ValidAnswer<T>) {
		flowOf(it)
	} else {
		emptyFlow()
	}
}
