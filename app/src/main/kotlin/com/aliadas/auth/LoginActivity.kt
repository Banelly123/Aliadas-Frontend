package com.aliadas.auth

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.aliadas.MainActivity
import com.aliadas.R
import com.aliadas.databinding.ActivityLoginBinding
import com.aliadas.network.LoginRequest
import com.aliadas.network.RegisterRequest
import com.aliadas.network.RetrofitClient
import com.aliadas.utils.SessionManager
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding
    private var isLoginMode = true
    private var isPasswordVisible = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Si ya cuenta con sesión activa, saltamos de inmediato a la pantalla principal
        val token = SessionManager.getBearerToken(this)
        if (!token.isNullOrEmpty()) {
            startMain()
            return
        }

        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupClickListeners()
    }

    private fun setupClickListeners() {
        // Botón de acción unificado ("Iniciar Sesión" / "Registrarse")
        binding.btnSubmitLogin.setOnClickListener {
            if (isLoginMode) doLogin() else doRegister()
        }

        // Enlace inferior para alternar dinámicamente entre pantallas
        binding.txtRegisterRedirect.setOnClickListener {
            toggleMode()
        }

        // Control visual interactivo para revelar u ocultar la contraseña
        binding.btnToggleLoginPassword.setOnClickListener {
            togglePasswordVisibility()
        }

        // Enlace para recuperar credenciales (Marcador de posición para desarrollo futuro)
        binding.txtForgotPassword.setOnClickListener {
            Toast.makeText(this, "Función de recuperación en desarrollo... 💜", Toast.LENGTH_SHORT).show()
        }

        // Simulación para el botón externo de Google
        binding.btnGoogleLogin.setOnClickListener {
            Toast.makeText(this, "Conectando con Google Services...", Toast.LENGTH_SHORT).show()
        }
    }

    /**
     * Alterna la interfaz entre el modo de ingreso estándar y el formulario de alta
     */
    private fun toggleMode() {
        isLoginMode = !isLoginMode
        if (isLoginMode) {
            binding.tilFullName.visibility = View.GONE  // Ocultamos el campo del Nombre Completo
            binding.btnSubmitLogin.text = "Iniciar Sesión"
            binding.txtRegisterRedirect.text = "¿No tienes una cuenta? Regístrate aquí"
            binding.txtHeaderTitle.text = "Ingresar a la red"
            binding.txtForgotPassword.visibility = View.VISIBLE
        } else {
            binding.tilFullName.visibility = View.VISIBLE // Mostramos el campo del Nombre Completo
            binding.btnSubmitLogin.text = "Registrarse"
            binding.txtRegisterRedirect.text = "¿Ya tienes una cuenta? Inicia Sesión aquí"
            binding.txtHeaderTitle.text = "Crea tu cuenta"
            binding.txtForgotPassword.visibility = View.GONE
        }
    }

    private fun doLogin() {
        val email = binding.edtLoginEmail.text.toString().trim()
        val password = binding.edtLoginPassword.text.toString()

        if (email.isEmpty() || password.isEmpty()) {
            showError("Completa todos los campos")
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.login(LoginRequest(email, password))
                if (res.isSuccessful && res.body() != null) {
                    val body = res.body()!!
                    SessionManager.saveSession(this@LoginActivity, body.token, body.userId, body.name, body.avatarIcon)
                    Toast.makeText(this@LoginActivity, "¡Bienvenida, ${body.name}! 💜", Toast.LENGTH_SHORT).show()
                    startMain()
                } else {
                    showError("Credenciales incorrectas o usuario inexistente")
                }
            } catch (e: Exception) {
                android.util.Log.e("ALIADAS_DEBUG", "Error de red: ${e.localizedMessage}")
                showError("Error de conexión a Railway: ${e.localizedMessage}")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun doRegister() {
        val name = binding.edtFullName.text.toString().trim()
        val email = binding.edtLoginEmail.text.toString().trim()
        val password = binding.edtLoginPassword.text.toString()

        if (name.isEmpty() || email.isEmpty() || password.length < 6) {
            showError("El nombre, correo y contraseña deben tener mínimo 6 caracteres")
            return
        }

        setLoading(true)
        lifecycleScope.launch {
            try {
                val res = RetrofitClient.api.register(RegisterRequest(name, email, password))
                if (res.isSuccessful && res.body() != null) {
                    val body = res.body()!!
                    SessionManager.saveSession(this@LoginActivity, body.token, body.userId, body.name, body.avatarIcon)
                    Toast.makeText(this@LoginActivity, "¡Cuenta creada con éxito! ✨", Toast.LENGTH_SHORT).show()
                    startMain()
                } else {
                    showError("Este correo electrónico ya se encuentra registrado")
                }
            } catch (e: Exception) {
                showError("Error de conexión al registrar: ${e.localizedMessage}")
            } finally {
                setLoading(false)
            }
        }
    }

    private fun togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible
        if (isPasswordVisible) {
            binding.edtLoginPassword.inputType = InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
            binding.btnToggleLoginPassword.setImageResource(R.drawable.ic_visibility_off)
        } else {
            binding.edtLoginPassword.inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            binding.btnToggleLoginPassword.setImageResource(R.drawable.ic_visibility)
        }
        binding.edtLoginPassword.setSelection(binding.edtLoginPassword.text.length)
    }

    private fun startMain() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun showError(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_LONG).show()

    private fun setLoading(loading: Boolean) {
        binding.btnSubmitLogin.isEnabled = !loading
    }
}