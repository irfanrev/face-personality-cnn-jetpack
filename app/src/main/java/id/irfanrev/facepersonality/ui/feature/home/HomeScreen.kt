package id.irfanrev.facepersonality.ui.feature.home

import android.Manifest
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.graphics.drawable.Icon
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.launch
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import id.irfanrev.facepersonality.ml.Model
import id.irfanrev.facepersonality.util.reduceFileImage
import id.irfanrev.facepersonality.util.saveBitmapToFile
import id.irfanrev.facepersonality.util.saveUriToFile
import kotlinx.coroutines.Delay
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.tensorflow.lite.DataType
import org.tensorflow.lite.support.common.ops.NormalizeOp
import org.tensorflow.lite.support.image.ImageProcessor
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.support.image.ops.ResizeOp
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(

) {

    //val viewModel: FaceShapeViewModel = getViewModel()
    //val scaffoldState = rememberScaffoldState()
    val scope = rememberCoroutineScope()
    val (snackbarVisibleState, setSnackBarState) = remember { mutableStateOf(false) }
    var visible by remember {
        mutableStateOf(false)
    }
    var isPressed by remember {
        mutableStateOf(false)
    }

    var cameraPermissionGranted by remember {
        mutableStateOf(false)
    }

    val permissionLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.RequestPermission(), onResult = { isGranted ->
        if (isGranted) {
            Log.d("TAG" , "Permission $isGranted")
            cameraPermissionGranted = true
        }
    } )

    val context = LocalContext.current
    var imageUri by remember { mutableStateOf<Uri?>(null)}
    var bitmap  by remember{ mutableStateOf<Bitmap?>(null)}
    var imgFile  by remember{ mutableStateOf<File?>(null)}
    val coroutineScope = rememberCoroutineScope()
    var result by remember { mutableStateOf("") }
    var showDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()){ result ->
        if (result != null) {
            bitmap = result
            // Convert Bitmap to File
            imgFile = saveBitmapToFile(context, bitmap as Bitmap)
        }
    }

    val launcherGallery = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()){ result ->
        if (result != null) {
            imageUri = result
            // Convert Uri to File
            imgFile = saveUriToFile(context, imageUri as Uri)
        }
    }

    var imageProcessor = ImageProcessor.Builder()
        //.add(NormalizeOp(0.0f, 255.0f )))
        .add(ResizeOp(100, 100, ResizeOp.ResizeMethod.BILINEAR))
        .build()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {

        Text(
            text = "Face Personality",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(10.dp)
        )
        Text(
            text = "Find your personality based on your face\nUsing Machine Learning",
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(10.dp)
                .align(Alignment.CenterHorizontally),
        )
        Spacer(modifier = Modifier.height(20.dp))
        if (bitmap != null || imageUri != null) {

            imageUri?.let {
                bitmap = if(Build.VERSION.SDK_INT < 28){
                    MediaStore.Images.Media.getBitmap(context.contentResolver,it)
                }else {
                    val source = ImageDecoder.createSource(context.contentResolver,it)
                    ImageDecoder.decodeBitmap(source)
                }
            }
            Image(bitmap = bitmap?.asImageBitmap()!!, contentDescription = "",
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(320.dp)
                    .border(3.dp, Color.Gray, CircleShape)
                    .padding(10.dp)
                    .clip(
                        CircleShape
                    )
            )
            visible = true
        } else {
            Image(
                painter = painterResource(id = id.irfanrev.facepersonality.R.drawable.ic_face),
                contentDescription = "",
                contentScale = ContentScale.Inside,
                modifier = Modifier
                    .size(320.dp)
                    .border(3.dp, Color.Gray, CircleShape)
                    .padding(10.dp)
                    .clip(
                        CircleShape
                    )
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        if (!isPressed) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                Button(onClick = {
                    if (!cameraPermissionGranted) {
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    } else {
                        launcher.launch()
                    }
                }) {
                    Icon(
                        painter = painterResource(id = id.irfanrev.facepersonality.R.drawable.ic_camera),
                        contentDescription = "",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(text = "Ambil Gambar", color = Color.White)
                }

            }
        }

        if (bitmap != null) {
            Button(
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isPressed) Color.Green else Color.Gray,
                    contentColor = Color.White
                ),
                onClick = {
                    coroutineScope.launch {

                        if (isPressed) {
                            Log.d("TAG", "Pressed")
                            showDialog = showDialog.not()
                        } else {
                            isLoading = true
                            val file = reduceFileImage(imgFile as File)
                            Log.d("TAG", "File : $file")

                            var tensorImage = TensorImage(DataType.FLOAT32)
                            tensorImage.load(bitmap)
                            tensorImage = imageProcessor.process(tensorImage)

                            val model = Model.newInstance(context)

                            // Creates inputs for reference.
                            val inputFeature0 = TensorBuffer.createFixedSize(intArrayOf(1, 100, 100, 3), DataType.FLOAT32)
                            inputFeature0.loadBuffer(tensorImage.buffer)

                            // Runs model inference and gets result.
                            val outputs = model.process(inputFeature0)
                            val outputFeature0 = outputs.outputFeature0AsTensorBuffer.floatArray

                            var maxIndex = 0
                            //shapes = ['circle', 'heart', 'oblong', 'oval', 'square', 'triangle']
                            val shapes = listOf("circle", "heart", "oblong", "oval", "square", "triangle")
                            for (i in outputFeature0.indices) {
                                if (outputFeature0[maxIndex] < outputFeature0[i]) {
                                    maxIndex = i
                                }
                            }
                            delay(2000)
                            Log.d("TAG", "Result : ${shapes[maxIndex]}")
                            result = shapes[maxIndex]
                            // Releases model resources if no longer used.
                            model.close()
                            isPressed = true
                            isLoading = false
                            //viewModel.submitFaceShape(file)
                        }
                    }
                },
                shape = RoundedCornerShape(10),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 32.dp)
                    .height(57.dp)
            ) {
                if (isPressed) {
                    Text(text = "Lihat Hasil", style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold))
                } else {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier
                            .size(30.dp))
                    } else {
                        Text(text = "Prediksi", style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold))
                    }
                }
//            Text(text = "Predict", style = MaterialTheme.typography.titleMedium.copy(color = Color.White, fontWeight = FontWeight.Bold))
            }
        }


        if (showDialog) {
            AlertDialog(
                onDismissRequest = { showDialog = false },
                title = { Text("Bentuk Wajah Anda Adalah $result") },
                text = {
                    when (result) {
                        "circle" -> {
                            Text(text = "Cenderung ramah dan mudah berbaur dalam berbagai kelompok sosial.\n" +
                                    "Biasanya memiliki empati yang tinggi terhadap perasaan orang lain.\n" +
                                    "Bisa menjadi teman yang baik karena kemampuan mendengarkan dan memberikan dukungan.\n" +
                                    "Lebih suka bekerja dalam tim daripada menjadi pemimpin tunggal.\n" +
                                    "Memiliki selera seni yang kuat dan cenderung menyukai seni visual.")
                        }
                        "heart" -> {
                            Text(text = "Penuh semangat dan ekspresif dalam mengungkapkan perasaan.\n" +
                                    "Sering menjadi pemberi dukungan dan peduli terhadap kebahagiaan orang lain.\n" +
                                    "Memiliki antusiasme yang tinggi dan cenderung mengejar impian mereka dengan tekun.\n" +
                                    "Cenderung menjadi pribadi yang percaya diri dan karismatik.\n" +
                                    "Suka membantu orang lain dan peduli tentang kesejahteraan mereka.")
                        }
                        "oblong" -> {
                            Text(text = "Terkenal dengan sifat mandiri dan fokus pada tujuan.\n" +
                                    "Biasanya menjadi individu yang tekun dan memiliki daya tahan yang tinggi.\n" +
                                    "Cenderung bekerja keras untuk mencapai kesuksesan.\n" +
                                    "Tidak mudah terganggu oleh perubahan atau tantangan.\n" +
                                    "Memiliki sifat-sifat pemimpin yang kuat.")
                        }
                        "oval" -> {
                            Text(text = "Cenderung memiliki kepribadian yang seimbang dan stabil.\n" +
                                    "Mudah beradaptasi dengan perubahan dan berbagai situasi.\n" +
                                    "Biasanya menjadi mediator yang baik dalam konflik.\n" +
                                    "Cenderung memiliki kepala dingin dan dapat mengambil keputusan secara rasional.\n" +
                                    "Toleran dan terbuka terhadap sudut pandang orang lain.")
                        }
                        "square" -> {
                            Text(text = "Cenderung memiliki sifat yang terorganisir dan struktural.\n" +
                                    "Pemikir logis dan analitis.\n" +
                                    "Menonjol dalam mengatur dan mengelola tugas dan proyek.\n" +
                                    "Sering menjadi pemimpin yang kuat dalam tim atau kelompok.\n" +
                                    "Tegas dalam membuat keputusan.")
                        }
                        "triangle" -> {
                            Text(text = "Suka menjadi pusat perhatian dan menyenangi peran kreatif.\n" +
                                    "Cenderung berpikir di luar kotak dan menciptakan solusi inovatif.\n" +
                                    "Tidak takut untuk mengambil risiko dalam menghadapi tantangan.\n" +
                                    "Suka mengejar minat seni dan estetika.\n" +
                                    "Memiliki daya tarik yang unik dan mampu mempengaruhi orang lain.")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = {
                        showDialog = false
                        isPressed = false
                        bitmap = null
                        imgFile = null
                        result = ""
                    }) {
                        Text("Oke".uppercase())
                    }
                },
            )
        }



    }

}