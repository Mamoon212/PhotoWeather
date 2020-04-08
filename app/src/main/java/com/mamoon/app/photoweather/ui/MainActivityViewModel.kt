package com.mamoon.app.photoweather.ui

import android.app.Application
import android.content.Context
import android.graphics.*
import android.graphics.drawable.BitmapDrawable
import android.location.Geocoder
import android.location.Location
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.android.gms.location.LocationServices
import com.mamoon.app.photoweather.retrofit.Apifactory
import com.mamoon.app.photoweather.retrofit.Result
import com.mamoon.app.photoweather.room.Post
import com.mamoon.app.photoweather.room.PostDatabase
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*


class MainActivityViewModel(application: Application) : AndroidViewModel(application) {
    //coroutines variables
    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    //retrofit variables
    private val weatherApi = Apifactory.weatherApi

    //location variables
    private val fusedLocationClient =
        LocationServices.getFusedLocationProviderClient(getApplication<Application>())
    private val locale = Locale("en")
    private val geocoder = Geocoder(getApplication(), locale)

    //database variables
    private val dao = PostDatabase.getInstance(getApplication()).postDatabaseDao

    //livedata variables
    private val _startPhotoActivity = MutableLiveData<Boolean>()
    val startPhotoActivity: LiveData<Boolean>
        get() = _startPhotoActivity

    private val _photo = MutableLiveData<BitmapDrawable>()
    val photo: LiveData<BitmapDrawable>
        get() = _photo

    private val _reOpenPhoto = MutableLiveData<Uri>()
    val reOpenPhoto: LiveData<Uri>
        get() = _reOpenPhoto

    private val _showApiErrorMessage = MutableLiveData<String>()
    val showErrorMessage: LiveData<String>
        get() = _showApiErrorMessage

    private val _startShareIntent = MutableLiveData<Uri>()
    val startShareIntent: LiveData<Uri>
        get() = _startShareIntent

    private val _sharePhotoUri = MutableLiveData<Uri>()


    var posts = dao.getAllPosts()

    private val channel = Channel<Location?>()

    fun fabClicked() {
        _startPhotoActivity.value = true
    }

    fun doneStartingActivity() {
        _startPhotoActivity.value = false
    }

    //this function gets the location, the city name, the weather data, and the time, and the final image once done
    fun preparePhotoForDrawing(bitmap: Bitmap) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                getLoc()
                val location = channel.receive()
                location?.let {
                    val cityName = getCityName(it)
                    cityName?.let { city ->
                        val weather = fetchWeatherForCity(city)
                        weather?.let { result ->
                            val time = getTime()
                            val finalString = writeFinalString(city, result, time)
                            withContext(Dispatchers.Main) {
                                _photo.value =
                                    writeTextOnDrawable(getApplication(), bitmap, finalString)
                            }

                        }
                    }
                }
                if (location == null) {
                    withContext(Dispatchers.Main) {
                        _showApiErrorMessage.value =
                            "Couldn't fetch location, enable GPS and try again."
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
                _showApiErrorMessage.value = e.localizedMessage
            }

        }

    }

    private fun getLoc() {
        fusedLocationClient.lastLocation.addOnSuccessListener { loc ->
            uiScope.launch {
                channel.send(loc)
            }
        }

    }


    //uses the location to get the city name
    private suspend fun getCityName(location: Location): String? {
        return withContext(Dispatchers.IO) {
            try {
                val address = geocoder.getFromLocation(
                    location.latitude, location.longitude
                    , 1
                )
                val cityName =
                    address[0].adminArea.toLowerCase(locale).replace("governorate", "").trim()

                cityName
            } catch (e: IOException) {
                e.printStackTrace()
                uiScope.launch {
                    _showApiErrorMessage.value = "App needs Internet to work."

                }
                null
            }

        }
    }

    //uses the city name to get the weather data
    private suspend fun fetchWeatherForCity(city: String): Result? {
        return withContext(Dispatchers.IO) {
            try {
                val resultDeferred = weatherApi.getWeatherForCity(city)
                val result = resultDeferred.await()
                result
            } catch (e: IOException) {
                uiScope.launch {
                    _showApiErrorMessage.value =
                        "Problem fetching the weather data, check your connection."
                }
                null
            }
        }
    }

    //gets current time
    private fun getTime(): String {
        val dateTime = LocalDateTime.now()
        val formatter = DateTimeFormatter.ofPattern("hh:mm a")
        return dateTime.format(formatter)
    }

    //puts all the peices together and prepared the final string for drawing
    private fun writeFinalString(city: String, result: Result, time: String): String {
        return "$city/${result.weather[0].description}/${result.main.temp}°c - $time"
    }

    //does the actual drawing
    private suspend fun writeTextOnDrawable(
        context: Context, bm: Bitmap, text: String
    ): BitmapDrawable {
        return withContext(Dispatchers.IO) {
            val lines = text.split("/")

            val tf = Typeface.create("Roboto", Typeface.BOLD)

            val paint = Paint()
            paint.style = Paint.Style.FILL
            paint.color = Color.WHITE
            paint.typeface = tf
            paint.textAlign = Paint.Align.LEFT
            paint.textSize = convertToPixels(context, 200).toFloat()


            val textRect = Rect()
            paint.getTextBounds(text, 0, lines[2].length, textRect)

            val canvas = Canvas(bm)

            //If the text is bigger than the canvas , reduce the font size
            if (textRect.width() >= canvas.width - 4)
            //the padding on either sides is considered as 4, so as to appropriately fit in the text
                paint.textSize = convertToPixels(context, canvas.width / 23).toFloat()

            //Calculate the positions
            val xPos = canvas.width.toFloat() / 30

            //"- ((paint.descent() + paint.ascent()) / 2)" is the distance from the baseline to the center.
            val yPos = (canvas.height + (paint.descent() + paint.ascent()) * 3)
            var addVertical = 0
            var lineIndex = 0

            lines.forEach {
                val string = it
                val capitalizedString =
                    string.substring(0, 1).toUpperCase(locale) + string.substring(1)
                if (lineIndex > 0) {
                    paint.textSize = (paint.textSize * 0.7).toFloat()
                }
                canvas.drawText(capitalizedString, xPos, yPos + addVertical, paint)
                addVertical += 200
                lineIndex += 1
            }
            savePhoto(bm)
            BitmapDrawable(context.resources, bm)
        }
    }

    //saves the edited photo to the phone storage and to the database
    private suspend fun savePhoto(bm: Bitmap) {
        val file = File(
            getApplication<Application>().externalCacheDir.toString(),
            "fname_" + System.currentTimeMillis().toString() + ".jpg"
        )
        val uri = FileProvider.getUriForFile(
            getApplication(),
            "${getApplication<Application>().packageName}.fileprovider",
            file
        )
        withContext(Dispatchers.IO) {
            try {
                val fOut = FileOutputStream(file)
                bm.compress(Bitmap.CompressFormat.JPEG, 100, fOut)
                fOut.flush()
                fOut.close()
            } catch (e: IOException) {
                e.printStackTrace()
            }

            val post = Post(
                photoUri = uri.toString(), cityName = "test"
            )
            dao.insert(post)
        }
        uiScope.launch {
            _sharePhotoUri.value = uri
        }

    }

    //converts dp to actual pixels
    private fun convertToPixels(context: Context, nDP: Int): Int {
        val conversionScale = context.resources.displayMetrics.density
        return (nDP * conversionScale + 0.5f).toInt()
    }

    //fetches uri to open photo from history
    fun reOpenPhoto(post: Post) {
        val uri = Uri.parse(post.photoUri)
        _reOpenPhoto.value = uri
        _sharePhotoUri.value = uri
    }


    fun shareClicked() {
        _startShareIntent.value = _sharePhotoUri.value
    }

    fun doneSharing() {
        _startShareIntent.value = null
    }

    fun doneShowingErrorToast() {
        _showApiErrorMessage.value = null
    }

}