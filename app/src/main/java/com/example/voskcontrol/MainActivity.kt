package com.example.voskcontrol

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Toast
import android.widget.VideoView
import androidx.annotation.RawRes
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.example.voskcontrol.databinding.ActivityMainBinding
import org.json.JSONObject
import org.vosk.Model
import org.vosk.Recognizer
import org.vosk.android.RecognitionListener
import org.vosk.android.SpeechService
import org.vosk.android.StorageService
import java.io.IOException

// 数据类：定义一个动作，包含指令关键词和对应的视频资源ID
data class Action(val keywords: List<String>, @RawRes val videoResId: Int)

// 数据类：定义一个动物，包含它的名字、默认视频和所有支持的动作列表
data class Animal(val name: String, val keywords: List<String>, @RawRes val defaultVideoResId: Int, val actions: List<Action>)

class MainActivity : AppCompatActivity(), RecognitionListener {

    private lateinit var binding: ActivityMainBinding
    private lateinit var videoView: VideoView

    private var model: Model? = null
    private var speechService: SpeechService? = null
    private var isListening = false

    private lateinit var animals: List<Animal>
    private var currentAnimal: Animal? = null

    // 用于持久化存储
    private val prefsName = "AnimalPrefs"
    private val lastAnimalKey = "lastAnimalName"

    companion object {
        private const val TAG = "VoskControl"
        private const val PERMISSIONS_REQUEST_RECORD_AUDIO = 1
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or View.SYSTEM_UI_FLAG_FULLSCREEN)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        window.setBackgroundDrawable(null)
        videoView = binding.videoView
        videoView.setZOrderOnTop(true)

        initializeAnimals()
        checkAndRequestPermissions()
        Log.d(TAG, "MainActivity 已创建")
    }

    // 在这里集中管理所有的动物和指令
    private fun initializeAnimals() {
        val pomeranian = Animal("博美", listOf("博美", "小博美","我每","我没"), R.raw.pomeranian_happy, listOf(
            Action(listOf("不开心"), R.raw.pomeranian_unhappy),
            Action(listOf("穿红衣服"), R.raw.pomeranian_wear_red_clothes),
            Action(listOf("大喘气"), R.raw.pomeranian_pant),
            Action(listOf("弹吉他"), R.raw.pomeranian_play_guitar),
            Action(listOf("得瑟", "嘚瑟","特色","的是"), R.raw.pomeranian_cheerful),
            Action(listOf("喝奶"), R.raw.pomeranian_drink_milk),
            Action(listOf("开心", "笑一个"), R.raw.pomeranian_happy),
            Action(listOf("累了"), R.raw.pomeranian_tired),
            Action(listOf("难过"), R.raw.pomeranian_sad),
            Action(listOf("抛媚眼","眨眼睛","眨眼"), R.raw.pomeranian_wink),
            Action(listOf("舔爪子","便爪子","前爪子"), R.raw.pomeranian_lick_paw),
            Action(listOf("跳高高", "跳高"), R.raw.pomeranian_jump_high),
            Action(listOf("跳个舞", "跳舞"), R.raw.pomeranian_dance),
            Action(listOf("我饿了", "饿了"), R.raw.pomeranian_hungry),
            Action(listOf("转圈圈", "转圈"), R.raw.pomeranian_turn_circle)
        ))

        val shibaInu = Animal("柴犬", listOf("柴犬", "小柴","采取"), R.raw.shiba_inu_happy, listOf(
            Action(listOf("不开心"), R.raw.shiba_inu_unhappy),
            Action(listOf("穿绿衣", "穿衣服","创绿衣","翻窗绿衣"), R.raw.shiba_inu_wear_green_clothes),
            Action(listOf("打哈欠","法哈欠"), R.raw.shiba_inu_yawn),
            Action(listOf("弹吉它", "弹吉他"), R.raw.shiba_inu_play_guitar),
            Action(listOf("东张西望"), R.raw.shiba_inu_look_around),
            Action(listOf("喝奶","河南"), R.raw.shiba_inu_drink_milk),
            Action(listOf("开心"), R.raw.shiba_inu_happy),
            Action(listOf("难过","能够"), R.raw.shiba_inu_sad),
            Action(listOf("抛媚眼","眨眼睛"), R.raw.shiba_inu_wink),
            Action(listOf("舔爪子","便爪子","前爪子"), R.raw.shiba_inu_lick_paw),
            Action(listOf("跳高高", "跳高"), R.raw.shiba_inu_jump_high),
            Action(listOf("跳舞"), R.raw.shiba_inu_dance),
            Action(listOf("笑一个", "笑一笑"), R.raw.shiba_inu_smile),
            Action(listOf("摇尾巴"), R.raw.shiba_inu_wag_tail)
        ))

        val teddy = Animal("黑泰迪", listOf("泰迪", "黑泰迪"), R.raw.teddy_black_happy, listOf(
            Action(listOf("不开心"), R.raw.teddy_black_unhappy),
            Action(listOf("弹吉它", "弹吉他"), R.raw.teddy_black_play_guitar),
            Action(listOf("得瑟", "嘚瑟","特色","的是"), R.raw.teddy_black_cheerful),
            Action(listOf("喝奶","河南"), R.raw.teddy_black_drink_milk),
            Action(listOf("很不开心", "非常不开心","很伤心"), R.raw.teddy_black_very_unhappy),
            Action(listOf("开心"), R.raw.teddy_black_happy),
            Action(listOf("可怜巴巴", "装可怜"), R.raw.teddy_black_pitiful),
            Action(listOf("来个节奏"), R.raw.teddy_black_rhythm),
            Action(listOf("难过"), R.raw.teddy_black_sad),
            Action(listOf("受伤了"), R.raw.teddy_black_hurt),
            Action(listOf("舔爪子","便爪子","前爪子"), R.raw.teddy_black_lick_paw),
            Action(listOf("委屈"), R.raw.teddy_black_wronged),
            Action(listOf("我饿了", "饿了"), R.raw.teddy_black_hungry),
            Action( listOf("笑一个", "笑一笑"), R.raw.teddy_black_smile),
            Action(listOf("转个圈", "转圈"), R.raw.teddy_black_turn_circle)
        ))

        val goldenRetriever = Animal("金毛", listOf("金毛", "金毛犬"), R.raw.golden_retriever_happy, listOf(
            Action(listOf("饿了", "我饿了"), R.raw.golden_retriever_hungry),
            Action(listOf("好累", "累了"), R.raw.golden_retriever_tired),
            Action(listOf("喝奶","河南"), R.raw.golden_retriever_drink_milk),
            Action(listOf("开心", "笑一个"), R.raw.golden_retriever_happy),
            Action(listOf("看着我"), R.raw.golden_retriever_look_at_me),
            Action(listOf("难过"), R.raw.golden_retriever_sad),
            Action(listOf("舔手手", "舔手","田鼠"), R.raw.golden_retriever_lick_hand),
            Action(listOf("跳高高", "跳高"), R.raw.golden_retriever_jump_high),
            Action(listOf("捂鼻子","鼻子"), R.raw.golden_retriever_cover_nose),
            Action(listOf("兴奋"), R.raw.golden_retriever_excited),
            Action(listOf("转个圈", "转圈"), R.raw.golden_retriever_turn_circle),
            Action(listOf("走一个", "走两步"), R.raw.golden_retriever_walk)
        ))

        val nezha = Animal(
            name = "哪吒",
            keywords = listOf("哪吒", "小英雄"),
            defaultVideoResId = R.raw.nezha,
            actions = listOf()
        )

        val dinosaur = Animal(
            name = "恐龙",
            keywords = listOf("恐龙", "小恐龙"),
            defaultVideoResId = R.raw.dinosaur_angry, // 默认视频：愤怒
            actions = listOf(
                Action(listOf("愤怒", "生气"), R.raw.dinosaur_angry),
                Action(listOf("拿刀攻击", "拿刀","攻击"), R.raw.dinosaur_attack_with_knife),
                Action(listOf("中手雷", "手雷"), R.raw.dinosaur_hit_by_grenade),
                Action(listOf("中枪","中将"), R.raw.dinosaur_shot),
                Action(listOf("哒哒哒", "大大大", "打打打"), R.raw.dinosaur_rat_a_tat_tat)
            )
        )

        val grandpa = Animal(
            name = "老爷爷",
            keywords = listOf("老爷爷", "爷爷"),
            defaultVideoResId = R.raw.oldman,
            actions = listOf(
                Action(listOf("慈祥"), R.raw.oldman)
            )
        )

        animals = listOf(pomeranian, shibaInu, teddy, goldenRetriever, nezha, dinosaur, grandpa)
    }

    override fun onPartialResult(hypothesis: String) {
        if (!isListening) return

        try {
            val json = JSONObject(hypothesis)
            val partialText = json.getString("partial").replace(" ", "")
            if (partialText.isBlank()) return

            Log.d(TAG, "听到: $partialText")

            for (animal in animals) {
                for (keyword in animal.keywords) {
                    if (partialText.contains(keyword)) {
                        Log.i(TAG, "检测到动物切换指令: '${animal.name}'")
                        switchAnimal(animal)
                        speechService?.reset()
                        return
                    }
                }
            }

            currentAnimal?.let { animal ->
                for (action in animal.actions) {
                    for (keyword in action.keywords) {
                        if (partialText.contains(keyword)) {
                            Log.i(TAG, "在'${animal.name}'上下文中检测到动作: '${keyword}'")
                            playVideo(action.videoResId, isLooping = false)
                            speechService?.reset()
                            return
                        }
                    }
                }
            }
        } catch (e: Exception) { /* 忽略JSON解析错误 */ }
    }

    private fun switchAnimal(animal: Animal) {
        if (currentAnimal?.name == animal.name) {
            Log.d(TAG, "已经是'${animal.name}'，无需切换。")
            playVideo(animal.defaultVideoResId, isLooping = true)
            return
        }

        Log.d(TAG, "切换到 '${animal.name}'")
        currentAnimal = animal
        playVideo(animal.defaultVideoResId, isLooping = true)

        val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        prefs.edit().putString(lastAnimalKey, animal.name).apply()
        Log.d(TAG, "已将 '${animal.name}' 保存到偏好设置。")
    }

    private fun playVideo(@RawRes videoResId: Int, isLooping: Boolean) {
        try {
            val currentVideoTag = videoView.tag as? Int
            // 修复：如果正在播放的视频和新请求的视频是同一个，并且它是一个循环视频，则不打断
            if (videoView.isPlaying && currentVideoTag == videoResId && isLooping) {
                return
            }

            Log.d(TAG, "准备播放视频资源ID: $videoResId | 是否循环: $isLooping")
            videoView.stopPlayback()
            videoView.tag = videoResId
            val uri = Uri.parse("android.resource://$packageName/$videoResId")
            videoView.setVideoURI(uri)

            videoView.setOnPreparedListener { mp ->
                mp.isLooping = isLooping
                videoView.start()
            }

            if (!isLooping) {
                videoView.setOnCompletionListener {
                    Log.d(TAG, "动作视频播放完毕，恢复播放默认视频。")
                    currentAnimal?.let {
                        playVideo(it.defaultVideoResId, isLooping = true)
                    }
                }
            } else {
                videoView.setOnCompletionListener(null)
            }
        } catch (e: Exception) {
            Log.e(TAG, "播放视频时出错", e)
        }
    }

    private fun startDefaultBehavior() {
        startRecognition()

        val prefs = getSharedPreferences(prefsName, Context.MODE_PRIVATE)
        val lastAnimalName = prefs.getString(lastAnimalKey, null)
        val animalToStart = animals.find { it.name == lastAnimalName }
        currentAnimal = animalToStart ?: animals.firstOrNull()

        currentAnimal?.let {
            Log.d(TAG, "应用启动，当前动物: ${it.name}")
            playVideo(it.defaultVideoResId, isLooping = true)
        }
    }

    override fun onResume() {
        super.onResume()
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED && model != null && !isListening) {
            startRecognition()
        }
        if (!videoView.isPlaying) {
            currentAnimal?.let {
                playVideo(it.defaultVideoResId, isLooping = true)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        stopRecognition()
        if (videoView.isPlaying) {
            videoView.stopPlayback()
        }
    }

    private fun checkAndRequestPermissions() {
        val permission = Manifest.permission.RECORD_AUDIO
        if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(permission), PERMISSIONS_REQUEST_RECORD_AUDIO)
        } else {
            initVoskModel()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                initVoskModel()
            } else {
                Toast.makeText(this, "需要录音权限才能使用此应用", Toast.LENGTH_LONG).show()
                finish()
            }
        }
    }

    private fun initVoskModel() {
        if (model != null) {
            startDefaultBehavior()
            return
        }
        StorageService.unpack(this, "model-zh-cn", "model",
            { model ->
                this.model = model
                runOnUiThread {
                    Log.d(TAG, "模型加载成功！")
                    startDefaultBehavior()
                }
            },
            { exception ->
                runOnUiThread {
                    Log.e(TAG, "模型加载失败", exception)
                    Toast.makeText(this, "模型加载失败，请重启应用", Toast.LENGTH_LONG).show()
                }
            })
    }

    private fun startRecognition() {
        if (isListening) return
        if (model == null) {
            Log.e(TAG, "无法开始识别，模型为null")
            return
        }
        try {
            val grammar = animals.flatMap { it.keywords + it.actions.flatMap { action -> action.keywords } }.toSet()
            val grammarJson = "[\"${grammar.joinToString("\", \"")}\"]"
            val recognizer = Recognizer(model, 16000.0f, grammarJson)
            speechService = SpeechService(recognizer, 16000.0f)
            speechService?.startListening(this)
            isListening = true
            Log.d(TAG, ">>> 已开始聆听...")
        } catch (e: IOException) {
            Log.e(TAG, "启动监听失败", e)
        }
    }

    private fun stopRecognition() {
        if (!isListening) return
        speechService?.stop()
        isListening = false
        Log.d(TAG, "<<< 已停止聆听")
    }

    override fun onResult(hypothesis: String) { /* 不使用 */ }
    override fun onFinalResult(hypothesis: String) { /* 不使用 */ }

    override fun onTimeout() {
        if (isListening) {
            isListening = false
            Log.w(TAG, "聆听超时，正在重启...")
            startRecognition()
        }
    }

    override fun onError(exception: Exception) {
        Log.e(TAG, "识别出错", exception)
        if (isListening) {
            isListening = false
            Handler(Looper.getMainLooper()).postDelayed({ startRecognition() }, 1000)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        speechService?.stop()
        speechService?.shutdown()
        Log.d(TAG, "Activity已销毁")
    }
}