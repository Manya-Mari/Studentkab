package com.example.studentkab

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.studentkab.data.DatabaseProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Экран входа: студент вводит логин и пароль.
 * При успехе открывается [MainActivity] с данными студента.
 */
class LoginActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_login)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.loginScroll)) { view, insets ->
            val bars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            view.setPadding(view.paddingLeft, bars.top, view.paddingRight, bars.bottom)
            insets
        }

        @Suppress("DEPRECATION")
        window.statusBarColor = getColor(R.color.screen_background)
        WindowInsetsControllerCompat(window, window.decorView).isAppearanceLightStatusBars = true

        val db = DatabaseProvider.getDatabase(applicationContext)
        val loginInput   = findViewById<EditText>(R.id.inputLogin)
        val passwordInput = findViewById<EditText>(R.id.inputPassword)
        val loginButton  = findViewById<Button>(R.id.loginButton)
        val errorView    = findViewById<TextView>(R.id.loginError)

        // Не разрешаем вход, пока база данных не подготовлена.
        loginButton.isEnabled = false
        lifecycleScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    DatabaseProvider.insertTestDataIfNeeded(applicationContext, db)
                }
                loginButton.isEnabled = true
            } catch (error: Exception) {
                errorView.text = "Не удалось подготовить базу данных. Перезапустите приложение."
                errorView.visibility = View.VISIBLE
            }
        }

        loginButton.setOnClickListener {
            if (!loginButton.isEnabled) return@setOnClickListener
            val login    = loginInput.text.toString().trim()
            val password = passwordInput.text.toString()

            if (login.isEmpty()) {
                loginInput.error = "Введите логин"
                return@setOnClickListener
            }
            if (password.isEmpty()) {
                passwordInput.error = "Введите пароль"
                return@setOnClickListener
            }

            errorView.visibility = View.GONE
            loginButton.isEnabled = false
            loginButton.text = "Вход..."

            loginButton.isEnabled = false
            loginButton.text = "Проверка..."
            lifecycleScope.launch {
                try {
                    val student = withContext(Dispatchers.IO) {
                        db.studentDao().findByCredentials(login, password)
                    }
                    if (student == null) {
                        errorView.text = "Неверный логин или пароль. Проверьте раскладку и данные."
                        errorView.visibility = View.VISIBLE
                    } else {
                        val intent = Intent(this@LoginActivity, MainActivity::class.java).apply {
                            putExtra(MainActivity.EXTRA_STUDENT_ID, student.id)
                            putExtra(MainActivity.EXTRA_STUDENT_NAME, student.name)
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(intent)
                    }
                } catch (error: Exception) {
                    errorView.text = "Не удалось выполнить вход. Попробуйте ещё раз."
                    errorView.visibility = View.VISIBLE
                } finally {
                    loginButton.isEnabled = true
                    loginButton.text = "Войти"
                }
            }
        }
    }
}
