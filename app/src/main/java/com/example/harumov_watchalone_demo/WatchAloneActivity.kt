package com.example.harumov_watchalone_demo

import android.Manifest
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.SurfaceHolder
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.amazonaws.auth.AWSCredentials
import com.amazonaws.auth.BasicAWSCredentials
import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener
import com.amazonaws.mobileconnectors.s3.transferutility.TransferNetworkLossHandler
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility
import com.amazonaws.regions.Region
import com.amazonaws.regions.Regions
import com.amazonaws.services.s3.AmazonS3Client
import kotlinx.android.synthetic.main.activity_watch_alone.*
import java.io.File
import java.lang.Thread.sleep
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import kotlin.properties.Delegates

class WatchAloneActivity : AppCompatActivity(), SurfaceHolder.Callback {

    private var cameraThread: CameraThread? = null
    lateinit var cameraHandler: CameraHandler

    val WATCH_START = 0
    val WATCH_END = 1

    private var imageCapture: ImageCapture? = null

    private lateinit var outputDirectory: File
    private lateinit var cameraExecutor: ExecutorService

    private lateinit var surfaceHolder: SurfaceHolder
    private lateinit var mediaPlayer: MediaPlayer
    private var isPlayed by Delegates.notNull<Boolean>()

    // 현재 로그인하고 있는 사용자 아이디, 선택한 영화 제목, 선택한 영화 상영 시간
    lateinit var id : String
    lateinit var movie_title : String
    var running_time by Delegates.notNull<Int>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_watch_alone)

        id = intent.getStringExtra("user_id").toString()
        movie_title = intent.getStringExtra("movie_title").toString()
        running_time = intent.getIntExtra("running_time", 0)

        // 검색 페이지에서 전달받은 인텐트 데이터 확인
        if (intent.hasExtra("user_id") && intent.hasExtra("movie_title")) {
            Log.d("WatchAloneActivity", "검색에서 받아온 id : $id , movie title : $movie_title, " +
                    "running time : $running_time")
        } else {
            Log.e("WatchAloneActivity", "가져온 데이터 없음")
        }

        // Request camera permissions
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            ActivityCompat.requestPermissions(
                this, REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        cameraHandler = CameraHandler()

        isPlayed = false
        mediaPlayer = MediaPlayer()
        surfaceHolder = video_test_surfaceView.holder
        surfaceHolder.addCallback(this)

        // 감상시작 버튼 클릭
        watch_start.setOnClickListener {
            Log.d("감상 시작 : ", SimpleDateFormat("yyyy-MM-dd-HH-mm-ss-SSS", Locale.KOREA).format(System.currentTimeMillis()))

            takePhoto("capture", id + "_" + movie_title + "_" + "0", id, movie_title, "0")
            sleep(1000)
            takePhoto("capture", id + "_" + movie_title + "_" + "1", id, movie_title, "1")
            sleep(1000)
            takePhoto("capture", id + "_" + movie_title + "_" + "2", id, movie_title, "2")
            sleep(7000)

            if (cameraThread != null) {
                cameraThread!!.endThread()
            }
            cameraThread = CameraThread()
            cameraThread!!.start()

            mediaPlayer.start()
        }

        outputDirectory = getOutputDirectory()

        cameraExecutor = Executors.newSingleThreadExecutor()

        // 감상종료 버튼 클릭
        watch_end.setOnClickListener {
            cameraHandler.sendEmptyMessage(WATCH_END)
            Log.d("감상 : ", "종료되었습니다.")

            val intent = Intent(applicationContext, AddreviewActivity::class.java)
            intent.putExtra("user_id", id)
            intent.putExtra("movie_title", movie_title)
            startActivity(intent)

            Log.d("감상 종료 버튼 : ", "클릭")
        }
    }

    inner class CameraThread : Thread() {
        var i = 0
        var count = 3
        var ended = false

        var id = intent.getStringExtra("user_id").toString()
        var movie_title = intent.getStringExtra("movie_title").toString()
        var running_time = intent.getIntExtra("running_time", 0)
        var running_time_sec = running_time * 60

        fun endThread() {
            ended = true
        }

        override fun run() {
            super.run()

            Log.d("WatchAlone Thread - ", "Running Time : " + running_time)

            while (!ended) {
                var message: Message = Message.obtain()
                message.what = WATCH_START


                // Running Time에 따라 종료
                if (running_time > 0) {
                    // 0 초 ~ 60 초 : 10 초마다 3n + 2 (n 단위 : 초)
                    // 1 분 이상 : 1 분마다 18n + 2 (n 단위 : 분)
                    //if (count < (3 * (running_time_sec / 10)) + 2) { // running_time 단위가 분일 경우
                    if (count < (3 * (running_time / 10)) + 2) { // running_time 단위가 초일 경우
                        takePhoto("capture", id + "_" + movie_title + "_" + (9 + i).toString(), id, movie_title, (9 + i).toString())
                        sleep(1000)
                        takePhoto("capture", id + "_" + movie_title + "_" + (10 + i).toString(), id, movie_title, (10 + i).toString())
                        sleep(1000)
                        takePhoto("capture", id + "_" + movie_title + "_" + (11 + i).toString(), id, movie_title, (11 + i).toString())
                        cameraHandler.sendMessage(message)
                        i += 10
                        sleep(8000)

                        count += 3
                        Log.d("Capture - ", "Running Time Count : " + count)
                    } else {
                        ended = true
                        Log.d("Running Time : ", "자동으로 종료되었습니다.")

                        cameraHandler.sendEmptyMessage(WATCH_END)
                        Log.d("감상 : ", "종료되었습니다.")

                        val intent = Intent(applicationContext, AddreviewActivity::class.java)
                        intent.putExtra("user_id", id)
                        intent.putExtra("movie_title", movie_title)
                        startActivity(intent)

                        Log.d("WatchAloneEnd : ", "감상 리뷰 작성 페이지로 이동")
                    }
                }
                else { // running_time <= 0
                    ended = true
                    Log.d("Running Time : ", "자동으로 종료되었습니다.")

                    cameraHandler.sendEmptyMessage(WATCH_END)
                    Log.d("감상 : ", "종료되었습니다.")

                    val intent = Intent(applicationContext, AddreviewActivity::class.java)
                    intent.putExtra("user_id", id)
                    intent.putExtra("movie_title", movie_title)
                    startActivity(intent)

                    Log.d("WatchAloneEnd : ", "감상 리뷰 작성 페이지로 이동")
                }
            }
        }
    }

    inner class CameraHandler : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            when (msg.what) {
                WATCH_START -> {

                }
                WATCH_END -> {
                    cameraThread?.endThread()
                }
                else -> {

                }
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<String>, grantResults:
        IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) { // 요청 코드가 올바른지 확인
            if (allPermissionsGranted()) { // 권한이 부여되면 startCamera() 함수 호출
                startCamera()
            } else { // 권한이 부여되지 않은 경우 사용자에게 권한이 부여되지 않았음을 알리는 Toast 메시지 표시
                Toast.makeText(this,
                    "Permissions not granted by the user.",
                    Toast.LENGTH_SHORT).show()
                finish()
            }
        }
    }

    private fun takePhoto(s3Bucket_FolderName: String?, fileName: String?, user_id: String?, movie_title: String?, time: String?) {
        // Get a stable reference of the modifiable image capture use case
        val imageCapture = imageCapture ?: return

        // Create time-stamped output file to hold the image
        val photoFile = File(outputDirectory, fileName + ".jpg") // 이미지를 저장할 파일을 만든다.

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Set up image capture listener, which is triggered after photo has been taken
        imageCapture.takePicture(
            outputOptions, ContextCompat.getMainExecutor(this), object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                    val savedUri = Uri.fromFile(photoFile)
                    val msg = "Photo capture succeeded: $savedUri"
//                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
                    Log.d(TAG, msg)
                    uploadWithTransferUtilty(s3Bucket_FolderName, photoFile.name, photoFile)
                }
            }
        )
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener(Runnable {
            // Used to bind the lifecycle of cameras to the lifecycle owner
            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

            // Preview
            val preview = Preview.Builder()
                .build()
                .also {
                    it.setSurfaceProvider(viewFinder.surfaceProvider)
                }

            imageCapture = ImageCapture.Builder()
                .build()

            // Select front camera as a default
            val cameraSelector = CameraSelector.DEFAULT_FRONT_CAMERA // 전면 카메라 // 후면 카메라 : DEFAULT_BACK_CAMERA

            try {
                // Unbind use cases before rebinding
                cameraProvider.unbindAll()

                // Bind use cases to camera
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture)

            } catch(exc: Exception) {
                Log.e(TAG, "Use case binding failed", exc)
            }

        }, ContextCompat.getMainExecutor(this))
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(
            baseContext, it) == PackageManager.PERMISSION_GRANTED
    }

    private fun getOutputDirectory(): File {
        val mediaDir = externalMediaDirs.firstOrNull()?.let {
            File(it, resources.getString(R.string.app_name)).apply { mkdirs() } }
        return if (mediaDir != null && mediaDir.exists())
            mediaDir else filesDir
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()

        mediaPlayer.release()
    }

    companion object {
        private const val TAG = "CameraXBasic"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
        private val REQUIRED_PERMISSIONS = arrayOf(Manifest.permission.CAMERA)
    }

    // S3 Bucket Upload
    fun uploadWithTransferUtilty(s3Bucket_FolderName: String?, fileName: String?, file: File?) {
        val awsCredentials: AWSCredentials =
            BasicAWSCredentials("access_Key", "secret_Key") // IAM User의 (accessKey, secretKey)
        val s3Client = AmazonS3Client(awsCredentials, Region.getRegion(Regions.AP_NORTHEAST_2))
        val transferUtility =
            TransferUtility.builder().s3Client(s3Client).context(this.applicationContext).build()
        TransferNetworkLossHandler.getInstance(this.applicationContext)
        val uploadObserver =
            transferUtility.upload("bucket_Name/" + s3Bucket_FolderName, fileName, file) // (bucket name, file 이름, file 객체)
        uploadObserver.setTransferListener(object : TransferListener {
            override fun onStateChanged(s3_id: Int, state: TransferState) {
                if (state === TransferState.COMPLETED) {
                    // Handle a completed upload
                    Log.d("S3 Bucket ", "Upload Completed!")

                    // S3 Bucket에 file 업로드 후 Emulator에서 삭제
                    if (file != null) {
                        file.delete()
                        Log.d("Emulator : ", "파일 삭제")
                    }
                    else {
                        Log.d("Emulator : ", "삭제할 파일이 없습니다.")
                    }
                }
            }

            override fun onProgressChanged(id: Int, current: Long, total: Long) {
                val done = (current.toDouble() / total * 100.0).toInt()
                Log.d("MYTAG", "UPLOAD - - ID: \$id, percent done = \$done")
            }

            override fun onError(id: Int, ex: java.lang.Exception) {
                Log.d("MYTAG", "UPLOAD ERROR - - ID: \$id - - EX:$ex")
            }
        })
    }

    override fun surfaceCreated(holder : SurfaceHolder) {
        val awsCredentials : AWSCredentials = BasicAWSCredentials("access_Key", "secret_Key") // IAM User의 (accessKey, secretKey)
        val s3Client = AmazonS3Client(awsCredentials, Region.getRegion(Regions.AP_NORTHEAST_2))

        var bucketUrl = s3Client.getResourceUrl("bucket_Name", null).toString()
        var videoName : String? = "video_Name.mp4"

        try {
            mediaPlayer.setDataSource(bucketUrl + videoName)
            mediaPlayer.setDisplay(holder)
            mediaPlayer.prepare()

            Log.d("MediaPlayer : ", bucketUrl + videoName + " 재생")
        }
        catch (e: Exception) {
            Log.e("Exception : ", e.toString())
        }
    }

    override fun surfaceChanged(holder : SurfaceHolder, format : Int, width : Int, height : Int) {

    }

    override fun surfaceDestroyed(holder : SurfaceHolder) {

    }

    override fun onUserLeaveHint() { // 홈 버튼 감지
        super.onUserLeaveHint()

        Log.d("Home Button : ", "이벤트 감지")

        if (isPlayed) {
            // 이벤트 작성
            mediaPlayer.release() // 홈 버튼 누른 후 다시 돌아오면 영상 종료된다. 그 후 다시 '시작' 버튼을 누르면 E/AndroidRuntime: FATAL EXCEPTION: main  Process: com.example.s3urlmediapractice, PID: 9545  java.lang.IllegalStateException: java.lang.IllegalStateException 발생

            var intent = Intent(applicationContext, AddreviewActivity::class.java)
//        startActivity(intent) // 인텐트로 페이지 이동 시 홈 버튼을 누른 후 이동할 페이지로 앱이 자동으로 다시 뜬다.

            val builder = AlertDialog.Builder(this)
            builder.setTitle("감상 종료")
                .setMessage("영화 감상이 종료됩니다.")
                .setPositiveButton("확인",
                    DialogInterface.OnClickListener { dialog, id ->
                    startActivity(intent) // 인텐트로 페이지 이동 시 페이지 이동 기록이 남아서 뒤로 가기 버튼을 누르면 이 페이지로 다시 돌아온다.
//                        finish() // 액티비티 종료  // 페이지 이동 기록 남지 않는다.
                    })
                .setCancelable(false) // 뒤로 가기 버튼과 영역 외 클릭 시 Dialog가 사라지지 않도록 한다.
            // Dialog 띄워 주기
            builder.show()
        }
        else {
            val builder = AlertDialog.Builder(this)
            builder.setTitle("영화 재선택")
                .setMessage("영화를 다시 선택해 주세요.")
                .setPositiveButton("확인",
                    DialogInterface.OnClickListener { dialog, id ->
//                    startActivity(intent) // 인텐트로 페이지 이동 시 페이지 이동 기록이 남아서 뒤로 가기 버튼을 누르면 이 페이지로 다시 돌아온다.
                        finish() // 액티비티 종료  // 페이지 이동 기록 남지 않는다.
                    })
                .setCancelable(false) // 뒤로 가기 버튼과 영역 외 클릭 시 Dialog가 사라지지 않도록 한다.
            // Dialog 띄워 주기
            builder.show()
        }

/*
        // 앱 종료
		System.exit(0) // 방법 1
    	android.os.Process.killProcess(android.os.Process.myPid()) // 방법 2
*/
    }
}