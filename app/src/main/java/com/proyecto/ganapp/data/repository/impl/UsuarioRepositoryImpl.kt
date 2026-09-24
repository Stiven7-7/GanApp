package com.proyecto.ganapp.data.repository.impl

import android.database.sqlite.SQLiteConstraintException
import com.proyecto.ganapp.data.local.dao.UsuarioDao
import com.proyecto.ganapp.data.local.entity.UsuarioEntity
import com.proyecto.ganapp.data.mapper.toDomain
import com.proyecto.ganapp.domain.model.Usuario
import com.proyecto.ganapp.domain.repository.RegisterUserRepositoryResult
import com.proyecto.ganapp.domain.repository.UsuarioRepository
import com.proyecto.ganapp.util.DateUtils
import com.proyecto.ganapp.util.PasswordUtils
import javax.inject.Inject

class UsuarioRepositoryImpl @Inject constructor(
    private val dao: UsuarioDao
) : UsuarioRepository {

    override suspend fun register(usuario: Usuario): RegisterUserRepositoryResult {
        // 🔒 Cifrar contraseña antes de guardar
        val hash = PasswordUtils.hash(usuario.contrasena)

        val entity = UsuarioEntity(
            nombre = usuario.nombre,
            apellido = usuario.apellido,
            correo = usuario.correo,
            contrasena = hash,
            fechaRegistro = DateUtils.currentDate(),
            activo = true
        )

        return try {
            val userId = dao.insert(entity)
            RegisterUserRepositoryResult.Success(userId)
        } catch (e: SQLiteConstraintException) {
            if (dao.getByCorreo(entity.correo) != null) {
                RegisterUserRepositoryResult.DuplicateEmail
            } else {
                throw e
            }
        }
    }

    override suspend fun login(correo: String, contrasena: String): Usuario? {
        // ✅ Recuperar usuario por correo
        val entity = dao.getByCorreo(correo) ?: return null

        // ✅ Verificar contraseña cifrada
        val verified = PasswordUtils.verify(contrasena, entity.contrasena)

        return if (verified) entity.toDomain() else null
    }

    override suspend fun getUserById(id: Long): Usuario? {
        return dao.getById(id)?.toDomain()
    }

    override suspend fun getUsuarioByCorreo(correo: String): Usuario? {
        return dao.getByCorreo(correo)?.toDomain()
    }
}
