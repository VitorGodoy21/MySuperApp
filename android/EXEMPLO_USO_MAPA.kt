// EXEMPLO DE USO - Funcionalidade de Mapa de Acessos QR Code

// ============================================================================
// 1. NAVEGANDO PARA A TELA DE MAPA
// ============================================================================

// Na tela QrCodeScreen, o usuário clica no botão "Ver Mapa de Acessos"
// que chama:
val qrCodeId = "XTF9FopziwTWxRRscDkv"
navController.navigate("access_log_map/$qrCodeId")

// Ou através do método helper:
navController.navigateToAccessLogMap(qrCodeId)


// ============================================================================
// 2. ESTRUTURA DE DADOS NO FIREBASE
// ============================================================================

/*
Firestore Path: qrcodes/{qrCodeId}/access_logs/{logId}

Exemplo de documento:
{
  "city": "São Bernardo do Campo",
  "country": "BR",
  "latitude": -23.6939,
  "longitude": -46.565,
  "timestamp": Timestamp(2026-03-26T03:49:12.584Z),
  "language": "pt-BR",
  "timeZone": "America/Sao_Paulo",
  "platform": "Win32",
  "userAgent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64)",
  "pageUrl": "https://baila.space/qr/?id=XTF9FopziwTWxRRscDkv",
  "pagePath": "/qr/",
  "screenWidth": 1920,
  "screenHeight": 1080,
  "scanId": "scan_1774496952208_ofop4793",
  "sessionId": "session_1774496952208_nbfa43gn",
  "status": "completed",
  "method": "IPINFO",
  "qrCodeId": "XTF9FopziwTWxRRscDkv"
}
*/


// ============================================================================
// 3. FLUXO NO VIEWMODEL
// ============================================================================

// O ViewModel inicia com estado padrão:
data class AccessLogMapUiState(
    val isLoading: Boolean = false,
    val accessLogs: List<AccessLog> = emptyList(),
    val cityStatistics: List<CityAccessStatistics> = emptyList(),
    val selectedCity: String? = null,
    val errorMessage: String = "",
    val viewMode: MapViewMode = MapViewMode.MAP  // ou CITY_LIST
)

// Quando a tela carrega, dispara:
viewModel.onEvent(AccessLogMapEvent.OnLoadAccessLogs(qrCodeId))

// Isso:
// 1. Busca todos os access logs do Firebase
// 2. Converte DTOs em Domain Models (AccessLog)
// 3. Agrupa por cidade para CityAccessStatistics
// 4. Atualiza o estado


// ============================================================================
// 4. MODOS DE VISUALIZAÇÃO
// ============================================================================

// MODO MAPA (MapViewMode.MAP)
// - Exibe Google Map Composable
// - Plots pins em latitude/longitude
// - Cada pin tem:
//   * Título: city (ex: "São Bernardo do Campo")
//   * Snippet: country + timestamp (ex: "BR - 26/03/2026 03:49")
// - Botão para alternar para lista de cidades
// - Botão para limpar filtro se alguma cidade estiver selecionada

// MODO LISTA DE CIDADES (MapViewMode.CITY_LIST)
// - Lista de cards, um por cidade
// - Cada card mostra:
//   * Ícone de localização
//   * Nome da cidade
//   * País
//   * Número de acessos (badge)
//   * Última data de acesso
// - Clicável para filtrar mapa por aquela cidade


// ============================================================================
// 5. INTERAÇÕES DO USUÁRIO
// ============================================================================

// Selecionar uma cidade
viewModel.onEvent(AccessLogMapEvent.OnSelectCity("São Bernardo do Campo"))
// Resultado: 
// - Filtra os pins do mapa para apenas essa cidade
// - Exibe card com cidade selecionada
// - Mostra botão "X" para limpar filtro

// Limpar filtro
viewModel.onEvent(AccessLogMapEvent.OnClearCityFilter)
// Resultado: selectedCity = null, exibe todos os pins novamente

// Alternar entre mapa e lista
viewModel.onEvent(AccessLogMapEvent.OnToggleViewMode)
// Resultado: viewMode muda entre MAP e CITY_LIST


// ============================================================================
// 6. CONVERSÃO DE DADOS (MAPPERS)
// ============================================================================

// DTO → Domain Model
val dto = QrCodeAccessLogDto(
    id = "log_1",
    qrCodeId = "qr_001",
    timestamp = Date(),
    latitude = -23.6939,
    longitude = -46.565,
    city = "São Bernardo do Campo",
    country = "BR",
    userAgent = "Mozilla/5.0",
    language = "pt-BR",
    timeZone = "America/Sao_Paulo"
)

val accessLog = dto.toAccessLog()
// Resultado: AccessLog com todos os campos mapeados

// Domain Model → Location
val location = accessLog.toAccessLocation()
// Resultado: AccessLocation com apenas coords, city, country
// (se latitude/longitude são null, retorna null)

// Agrupar por cidade
val logs = listOf(accessLog1, accessLog2, accessLog3)
val cityStats = logs.groupByCityStatistics()
// Resultado:
// [
//   CityAccessStatistics(
//     city = "São Bernardo do Campo",
//     country = "BR",
//     accessCount = 2,
//     lastAccessTime = Date mais recente,
//     latitude = -23.6939,
//     longitude = -46.565
//   ),
//   ...
// ]


// ============================================================================
// 7. USE CASES (Injeção de Dependência)
// ============================================================================

// GetAccessLogsWithLocationsUseCase
val accessLogs = getAccessLogsWithLocationsUseCase("qr_001")
// Retorna: List<AccessLog> convertido de DTOs
// Filtra apenas logs com latitude/longitude não-null

// GetCityAccessStatisticsUseCase  
val cityStats = getCityAccessStatisticsUseCase("qr_001")
// Retorna: List<CityAccessStatistics> agrupado por cidade
// Ordenado por número de acessos (decrescente)

// GetAccessLogsByCityUseCase
val logsInCity = getAccessLogsByCityUseCase("qr_001", "São Bernardo do Campo")
// Retorna: List<AccessLog> filtrado por cidade
// Case-insensitive


// ============================================================================
// 8. CONFIGURAÇÃO DO GOOGLE MAPS
// ============================================================================

// No AndroidManifest.xml, adicionar:
/*
<meta-data
    android:name="com.google.android.geo.API_KEY"
    android:value="YOUR_GOOGLE_MAPS_API_KEY_HERE" />
*/

// Chave pode estar em:
// 1. local.properties (desenvolvido)
// 2. Gradle buildConfigField (produção)
// 3. Variáveis de ambiente

// Cobertura de maps:
val cameraPositionState = rememberCameraPositionState {
    position = if (accessLogs.isNotEmpty()) {
        val firstLog = accessLogs.first()
        // Centraliza no primeiro acesso
        CameraPosition.fromLatLngZoom(
            LatLng(firstLog.latitude!!, firstLog.longitude!!),
            11f  // zoom level
        )
    } else {
        // Fallback: Brasil inteiro
        CameraPosition.fromLatLngZoom(LatLng(-15.7942, -47.8822), 4f)
    }
}

// Adicionar pins
accessLogs.filter { it.latitude != null && it.longitude != null }
.forEach {
    log ->
    Marker(
        state = MarkerState(position = LatLng(log.latitude!!, log.longitude!!)),
        title = log.city,
        snippet = "${log.country} - ${log.timestamp}"
    )
}


// ============================================================================
// 9. FORMATAÇÃO DE DADOS
// ============================================================================

// Uso de AccessLogFormatter para exibição
val timestamp = AccessLogFormatter.formatTimestamp(date)
// Retorna: "26/03/2026 03:49"

val dateOnly = AccessLogFormatter.formatDateOnly(date)
// Retorna: "26/03/2026"

val relativeTime = AccessLogFormatter.getRelativeTime(date)
// Retorna: "2 horas atrás" ou "26/03/2026"

val count = AccessLogFormatter.formatAccessCount(5)
// Retorna: "5 acessos"


// ============================================================================
// 10. TRATAMENTO DE ERROS
// ============================================================================

// No ViewModel, erros são capturados:
try {
    val logs = getAccessLogsWithLocationsUseCase(qrCodeId)
    // ... processar
} catch (e: Exception) {
    uiState.value = uiState.value.copy(
        isLoading = false,
        errorMessage = e.message ?: "Erro desconhecido"
    )
}

// Na tela, exibir mensagem:
if (uiState.errorMessage.isNotEmpty() && uiState.accessLogs.isEmpty()) {
    Text("Erro: ${uiState.errorMessage}")
}

// Effects (Toasts/Snackbars):
viewModel.effect.collect {
    effect ->
    when (effect) {
        is AccessLogMapEffect.ShowToast -> {
            snackBarHost.showSnackbar(effect.message)
        }

        is AccessLogMapEffect.ShowError -> {
            // Exibir erro em lugar destacado
        }
    }
}


// ============================================================================
// 11. TESTES
// ============================================================================

// Teste do ViewModel
@Test
fun testLoadAccessLogs_Success() = runTest {
    val qrCodeId = "test_qr"
    val mockLogs = listOf(AccessLog(...))
    val mockStats = listOf(CityAccessStatistics(...))

    `when`(getAccessLogsUseCase(qrCodeId)).thenReturn(mockLogs)
    `when`(getCityStatsUseCase(qrCodeId)).thenReturn(mockStats)

    viewModel.onEvent(AccessLogMapEvent.OnLoadAccessLogs(qrCodeId))

    val state = viewModel.uiState.first()
    assert(!state.isLoading)
    assert(state.accessLogs.size == 1)
}

// Teste do Mapper
@Test
fun testGroupByCityStatistics_AggregatesCorrectly() {
    val logs = listOf(
        AccessLog("1", "qr_001", Date(), -23.69, -46.56, "SBDO", "BR", "UA"),
        AccessLog("2", "qr_001", Date(), -23.69, -46.56, "SBDO", "BR", "UA"),
        AccessLog("3", "qr_001", Date(), -23.55, -46.63, "SP", "BR", "UA")
    )

    val stats = logs.groupByCityStatistics()

    assert(stats.size == 2)
    assert(stats[0].accessCount == 2)  // SBDO tem 2
    assert(stats[1].accessCount == 1)  // SP tem 1
}


// ============================================================================
// 12. PERFORMANCE
// ============================================================================

// Para QR Codes com muitos acessos (1000+):

// 1. Limite de registros carregados
override suspend fun getAccessLogsByQrCode(
    qrCodeId: String,
    limit: Int = 1000  // ← Limita a 1000 registros
): List<QrCodeAccessLogDto>?

// 2. Implementar clustering (futuro)
// val clusterManager = ClusterManager(context, googleMap)
// clusterManager.addItems(pins)

// 3. Paginação (futuro)
// offset e limit para carregar mais sob demanda


// ============================================================================
// 13. ESTRUTURA DE PASTAS FINAL
// ============================================================================

/*
app/src/
├── main/java/com/vfdeginformatica/mysuperapp/
│   ├── domain/
│   │   ├── model/
│   │   │   └── AccessLog.kt (novo)
│   │   ├── repository/
│   │   │   └── QrCodeAccessLogRepository.kt (expandido)
│   │   └── use_case/qrcode/
│   │       ├── GetAccessLogsWithLocationsUseCase.kt (novo)
│   │       ├── GetCityAccessStatisticsUseCase.kt (novo)
│   │       └── GetAccessLogsByCityUseCase.kt (novo)
│   ├── data/
│   │   ├── mapper/
│   │   │   └── AccessLogMapper.kt (novo)
│   │   └── remote/
│   │       ├── dto/
│   │       │   └── QrCodeAccessLogDto.kt (expandido)
│   │       └── repository/
│   │           └── QrCodeAccessLogRepositoryImpl.kt (expandido)
│   └── presentation/
│       └── screen/access_log_map/ (novo)
│           ├── AccessLogMapScreen.kt
│           ├── AccessLogMapViewModel.kt
│           ├── AccessLogMapRoute.kt
│           ├── util/
│           │   └── AccessLogFormatter.kt
│           └── contract/
│               └── AccessLogMapContract.kt
├── test/java/.../
│   ├── AccessLogMapViewModelTest.kt (novo)
│   └── AccessLogMapperTest.kt (novo)
└── ...

gradle/
└── libs.versions.toml (atualizado com Google Maps)
*/

