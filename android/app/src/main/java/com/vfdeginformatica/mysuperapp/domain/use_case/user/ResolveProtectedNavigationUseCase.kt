package com.vfdeginformatica.mysuperapp.domain.use_case.user

import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import javax.inject.Inject

/**
 * Resultado da resolução de uma navegação que pode exigir biometria.
 */
sealed interface ProtectedNavigationResult {
    data class Allowed(val route: String) : ProtectedNavigationResult
    data class Denied(val message: String) : ProtectedNavigationResult
}

/**
 * Fluxo comum reutilizável por qualquer tela/menu que precise proteger uma
 * navegação com biometria (ex.: Home e o menu lateral).
 *
 * Se [passwordRequired] for falso, a navegação é liberada diretamente.
 * Se for verdadeiro, exige uma [activity] válida e sucesso na autenticação
 * biométrica antes de liberar a rota.
 */
class ResolveProtectedNavigationUseCase @Inject constructor(
    private val authenticateWithBiometricUseCase: AuthenticateWithBiometricUseCase
) {
    operator fun invoke(
        route: String,
        passwordRequired: Boolean,
        activity: FragmentActivity?
    ): Flow<ProtectedNavigationResult> {
        if (!passwordRequired) {
            return flowOf(ProtectedNavigationResult.Allowed(route))
        }

        if (activity == null) {
            return flowOf(
                ProtectedNavigationResult.Denied("Erro: Não foi possível obter o contexto da aplicação")
            )
        }

        return authenticateWithBiometricUseCase(activity).map { isAuthenticated ->
            if (isAuthenticated) {
                ProtectedNavigationResult.Allowed(route)
            } else {
                ProtectedNavigationResult.Denied("Autenticação biométrica falhou")
            }
        }
    }
}
