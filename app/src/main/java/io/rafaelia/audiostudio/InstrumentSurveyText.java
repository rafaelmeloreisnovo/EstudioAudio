/*
 * Copyright (c) 2026 Rafael Melo Reis.
 * SPDX-License-Identifier: LicenseRef-RAFCODE-Research-Commercial-0.1
 */
package io.rafaelia.audiostudio;

import java.util.Locale;

/** Four-language presentation boundary. Export schema tokens remain language-neutral. */
public final class InstrumentSurveyText {
    private final int language; // 0 pt, 1 en, 2 es, 3 fr

    public InstrumentSurveyText(Locale locale) {
        String code = locale == null ? "pt" : locale.getLanguage();
        language = "en".equals(code) ? 1 : "es".equals(code) ? 2 : "fr".equals(code) ? 3 : 0;
    }

    public String languageCode() { return new String[]{"pt-BR", "en", "es", "fr"}[language]; }
    private String p(String pt, String en, String es, String fr) {
        return language == 1 ? en : language == 2 ? es : language == 3 ? fr : pt;
    }

    public String t(String key) {
        switch (key) {
            case "title": return p("Varredura de instrumentos", "Instrument survey", "Barrido de instrumentos", "Relevé des instruments");
            case "intro": return p("Escolha família, grupo ou item. A sessão observa apenas enquanto esta tela está aberta. GPS usa atualizações em primeiro plano; Wi-Fi e celular usam dados em cache. O aplicativo não altera rádios nem acessa o ServiceMode privado.", "Choose a family, group or item. Observation runs only while this screen is open. GPS uses foreground updates; Wi-Fi and cellular use cached data. The app does not change radios or access private ServiceMode data.", "Elija una familia, grupo o elemento. La observación solo funciona mientras esta pantalla está abierta. El GPS usa actualizaciones en primer plano; Wi-Fi y celular usan datos en caché. La aplicación no modifica radios ni accede al ServiceMode privado.", "Choisissez une famille, un groupe ou un élément. L'observation fonctionne uniquement lorsque cet écran est ouvert. Le GPS utilise des mises à jour au premier plan; le Wi-Fi et le réseau cellulaire utilisent des données en cache. L'application ne modifie pas les radios et n'accède pas au ServiceMode privé.");
            case "learning_help": return p("Aprendizado significa resumos estatísticos locais por item e setor de orientação. Não equivale a diagnóstico, previsão acústica ou certificação industrial. Selecione o vetor de rotação para classificar a orientação.", "Learning means local statistical summaries by item and orientation sector. It is not a diagnosis, acoustic prediction or industrial certification. Select the rotation vector to classify orientation.", "Aprendizaje significa resúmenes estadísticos locales por elemento y sector de orientación. No equivale a diagnóstico, predicción acústica ni certificación industrial. Seleccione el vector de rotación para clasificar la orientación.", "L'apprentissage désigne des résumés statistiques locaux par élément et secteur d'orientation. Il ne constitue ni un diagnostic, ni une prédiction acoustique, ni une certification industrielle. Sélectionnez le vecteur de rotation pour classer l'orientation.");
            case "site": return p("Nome do ponto ou ambiente (opcional)", "Location or room name (optional)", "Nombre del punto o ambiente (opcional)", "Nom du point ou de l'environnement (facultatif)");
            case "window": return p("Janela de rotação dos dados (ms, 100–600000)", "Data rotation window (ms, 100–600000)", "Ventana de rotación de datos (ms, 100–600000)", "Fenêtre de rotation des données (ms, 100–600000)");
            case "rows": return p("Máximo de linhas por janela", "Maximum rows per window", "Máximo de filas por ventana", "Nombre maximal de lignes par fenêtre");
            case "rotations": return p("Janelas retidas (1–64; produto com linhas ≤ 20000)", "Retained windows (1–64; rows product ≤ 20000)", "Ventanas retenidas (1–64; producto con filas ≤ 20000)", "Fenêtres conservées (1–64; produit avec les lignes ≤ 20000)");
            case "minimum": return p("Mínimo de amostras aceitas para resumo (2–20000)", "Minimum accepted samples for summary (2–20000)", "Mínimo de muestras aceptadas para el resumen (2–20000)", "Nombre minimal d'échantillons acceptés pour le résumé (2–20000)");
            case "age": return p("Idade máxima da fonte (ms, 0–600000)", "Maximum source age (ms, 0–600000)", "Edad máxima de la fuente (ms, 0–600000)", "Âge maximal de la source (ms, 0–600000)");
            case "accuracy": return p("Incerteza horizontal máxima do GPS (metros)", "Maximum GPS horizontal uncertainty (metres)", "Incertidumbre horizontal máxima del GPS (metros)", "Incertitude horizontale GPS maximale (mètres)");
            case "rate": return p("Taxa solicitada dos sensores (Hz, 1–50)", "Requested sensor rate (Hz, 1–50)", "Frecuencia solicitada de los sensores (Hz, 1–50)", "Fréquence demandée des capteurs (Hz, 1–50)");
            case "duration": return p("Duração máxima da sessão (segundos, 1–600)", "Maximum session duration (seconds, 1–600)", "Duración máxima de la sesión (segundos, 1–600)", "Durée maximale de la session (secondes, 1–600)");
            case "sampling_help": return p("GPS: intervalo solicitado de 1 s. Contexto em cache: consulta a cada 2 s. Releituras do mesmo instante não entram novamente no aprendizado.", "GPS: requested interval 1 s. Cached context: queried every 2 s. Repeated readings with the same timestamp do not enter learning again.", "GPS: intervalo solicitado de 1 s. Contexto en caché: consulta cada 2 s. Las lecturas repetidas con la misma marca de tiempo no vuelven a entrar en el aprendizaje.", "GPS : intervalle demandé de 1 s. Contexte en cache : interrogation toutes les 2 s. Les lectures répétées avec le même horodatage ne sont pas réintégrées à l'apprentissage.");
            case "family": return p("Família", "Family", "Familia", "Famille");
            case "group": return p("Grupo", "Group", "Grupo", "Groupe");
            case "select_all": return p("Selecionar todos", "Select all", "Seleccionar todos", "Tout sélectionner");
            case "clear": return p("Limpar seleção", "Clear selection", "Limpiar selección", "Effacer la sélection");
            case "permissions": return p("Permitir localização ou Bluetooth selecionados", "Allow selected location or Bluetooth access", "Permitir ubicación o Bluetooth seleccionados", "Autoriser la localisation ou le Bluetooth sélectionnés");
            case "start": return p("Iniciar nova sessão", "Start new session", "Iniciar nueva sesión", "Démarrer une nouvelle session");
            case "stop": return p("Parar e guardar resumo", "Stop and save summary", "Detener y guardar resumen", "Arrêter et enregistrer le résumé");
            case "sensitive": return p("Incluir localização e rádio na exportação (pode identificar lugares)", "Include location and radio data in export (may identify places)", "Incluir ubicación y radio en la exportación (puede identificar lugares)", "Inclure la localisation et les données radio dans l'exportation (peut identifier des lieux)");
            case "export": return p("Exportar sessão parada: CSV e configuração em ZIP", "Export stopped session: CSV and configuration in ZIP", "Exportar sesión detenida: CSV y configuración en ZIP", "Exporter la session arrêtée : CSV et configuration dans un ZIP");
            case "history": return p("Ver resumos locais retidos", "View retained local summaries", "Ver resúmenes locales retenidos", "Afficher les résumés locaux conservés");
            case "choose": return p("Escolha os instrumentos e inicie uma sessão.", "Choose instruments and start a session.", "Elija instrumentos e inicie una sesión.", "Choisissez les instruments et démarrez une session.");
            case "collecting": return p("COLETANDO", "COLLECTING", "RECOLECTANDO", "COLLECTE");
            case "stopped": return p("SESSÃO PARADA", "SESSION STOPPED", "SESIÓN DETENIDA", "SESSION ARRÊTÉE");
            case "remaining": return p("s restantes", "s remaining", "s restantes", "s restantes");
            case "descriptive": return p("Aprendizado descritivo da sessão", "Descriptive session learning", "Aprendizaje descriptivo de la sesión", "Apprentissage descriptif de la session");
            case "accepted": return p("Amostras aceitas", "Accepted samples", "Muestras aceptadas", "Échantillons acceptés");
            case "rejected": return p("rejeitadas", "rejected", "rechazadas", "rejetés");
            case "retained": return p("Retidas", "Retained", "Retenidas", "Conservées");
            case "evicted": return p("removidas do anel", "evicted from ring", "eliminadas del anillo", "retirées de l'anneau");
            case "variance": return p("variância", "variance", "varianza", "variance");
            case "density": return p("densidade aceita", "accepted density", "densidad aceptada", "densité acceptée");
            case "sector": return p("setor", "sector", "sector", "secteur");
            case "not_available": return p("indisponível", "unavailable", "no disponible", "indisponible");
            case "more_export": return p("Mais itens estão disponíveis na exportação.", "More items are available in the export.", "Hay más elementos disponibles en la exportación.", "D'autres éléments sont disponibles dans l'exportation.");
            case "no_history": return p("Nenhum resumo local retido.", "No retained local summary.", "No hay resúmenes locales retenidos.", "Aucun résumé local conservé.");
            case "history_title": return p("Sessões anteriores: resumos descritivos", "Previous sessions: descriptive summaries", "Sesiones anteriores: resúmenes descriptivos", "Sessions précédentes : résumés descriptifs");
            case "close": return p("Fechar", "Close", "Cerrar", "Fermer");
            case "permission_ready": return p("Os itens selecionados não exigem novas permissões, ou elas já foram concedidas.", "Selected items require no new permissions, or they are already granted.", "Los elementos seleccionados no requieren permisos nuevos, o ya fueron concedidos.", "Les éléments sélectionnés ne nécessitent aucune nouvelle autorisation, ou elles sont déjà accordées.");
            case "permission_updated": return p("Permissões atualizadas. A recusa ou a localização aproximada serão registradas como indisponíveis quando a fonte exigir precisão.", "Permissions updated. Denial or approximate location will be recorded as unavailable when a source requires precision.", "Permisos actualizados. La denegación o la ubicación aproximada se registrarán como no disponibles cuando una fuente requiera precisión.", "Autorisations mises à jour. Un refus ou une localisation approximative sera enregistré comme indisponible lorsqu'une source exige de la précision.");
            case "select_one": return p("Selecione pelo menos um item.", "Select at least one item.", "Seleccione al menos un elemento.", "Sélectionnez au moins un élément.");
            case "config_rejected": return p("Configuração ou sessão recusada", "Configuration or session rejected", "Configuración o sesión rechazada", "Configuration ou session refusée");
            case "saved": return p("Resumo sem localização ou rádio guardado", "Summary without location or radio data saved", "Resumen sin ubicación ni radio guardado", "Résumé sans localisation ni données radio enregistré");
            case "save_failed": return p("Falha ao guardar o resumo", "Failed to save summary", "Error al guardar el resumen", "Échec de l'enregistrement du résumé");
            case "summary_unavailable": return p("Resumo indisponível", "Summary unavailable", "Resumen no disponible", "Résumé indisponible");
            case "stop_before_export": return p("Pare a sessão antes de exportar.", "Stop the session before exporting.", "Detenga la sesión antes de exportar.", "Arrêtez la session avant l'exportation.");
            case "exported": return p("Exportado para Downloads/RafaeliaAudio/Instrumentos", "Exported to Downloads/RafaeliaAudio/Instrumentos", "Exportado a Downloads/RafaeliaAudio/Instrumentos", "Exporté vers Downloads/RafaeliaAudio/Instrumentos");
            case "included": return p("incluídos", "included", "incluidos", "incluses");
            case "omitted": return p("omitidos", "omitted", "omitidos", "omises");
            case "export_failed": return p("Falha na exportação", "Export failed", "Error de exportación", "Échec de l'exportation");
            case "family_sensors": return p("Sensores", "Sensors", "Sensores", "Capteurs");
            case "family_location": return p("Localização", "Location", "Ubicación", "Localisation");
            case "family_radio": return p("Rádio", "Radio", "Radio", "Radio");
            case "family_system": return p("Sistema", "System", "Sistema", "Système");
            case "family_capabilities": return p("Capacidades", "Capabilities", "Capacidades", "Capacités");
            case "group_cell": return p("Celular: conexão servidora", "Cellular: serving connection", "Celular: conexión servidora", "Cellulaire : connexion de service");
            case "group_wifi": return p("Wi-Fi: dados em cache", "Wi-Fi: cached data", "Wi-Fi: datos en caché", "Wi-Fi : données en cache");
            case "group_bluetooth": return p("Bluetooth: adaptador", "Bluetooth: adapter", "Bluetooth: adaptador", "Bluetooth : adaptateur");
            case "group_clock": return p("Relógio", "Clock", "Reloj", "Horloge");
            case "group_manufacturer": return p("Diagnóstico do fabricante", "Manufacturer diagnostics", "Diagnóstico del fabricante", "Diagnostic du fabricant");
            case "group_geographic": return p("Base geográfica", "Geographic database", "Base geográfica", "Base géographique");
            case "unsupported_sensors": return p("Sensores não amostrados", "Unsampled sensors", "Sensores no muestreados", "Capteurs non échantillonnés");
            case "latitude": return p("Latitude", "Latitude", "Latitud", "Latitude");
            case "longitude": return p("Longitude", "Longitude", "Longitud", "Longitude");
            case "altitude": return p("Altitude elipsoidal", "Ellipsoidal altitude", "Altitud elipsoidal", "Altitude ellipsoïdale");
            case "horizontal_accuracy": return p("Incerteza horizontal estimada", "Estimated horizontal uncertainty", "Incertidumbre horizontal estimada", "Incertitude horizontale estimée");
            case "speed": return p("Velocidade", "Speed", "Velocidad", "Vitesse");
            case "bearing": return p("Rumo do deslocamento", "Travel bearing", "Rumbo del desplazamiento", "Cap du déplacement");
            case "satellites": return p("Satélites reportados", "Reported satellites", "Satélites reportados", "Satellites signalés");
            case "satellites_used": return p("Satélites usados na posição", "Satellites used in fix", "Satélites usados en la posición", "Satellites utilisés pour la position");
            case "mean_cn0": return p("C/N0 médio reportado", "Reported mean C/N0", "C/N0 medio reportado", "C/N0 moyen signalé");
            case "cell_id": return p("Identificador da célula", "Cell identifier", "Identificador de celda", "Identifiant de cellule");
            case "radio_technology": return p("Tecnologia de rádio", "Radio technology", "Tecnología de radio", "Technologie radio");
            case "country_code": return p("Código do país", "Country code", "Código de país", "Code pays");
            case "network_code": return p("Código da rede", "Network code", "Código de red", "Code réseau");
            case "network_digits": return p("Dígitos do código de rede", "Network code digits", "Dígitos del código de red", "Chiffres du code réseau");
            case "cell_area": return p("Área da célula", "Cell area", "Área de celda", "Zone cellulaire");
            case "physical_cell": return p("Identificador físico", "Physical cell identifier", "Identificador físico", "Identifiant physique");
            case "radio_channel": return p("Canal de rádio", "Radio channel", "Canal de radio", "Canal radio");
            case "cell_signal": return p("Sinal da célula servidora", "Serving cell signal", "Señal de la celda servidora", "Signal de la cellule de service");
            case "wifi_signal": return p("Sinal da rede de referência", "Reference network signal", "Señal de la red de referencia", "Signal du réseau de référence");
            case "wifi_frequency": return p("Frequência da rede de referência", "Reference network frequency", "Frecuencia de la red de referencia", "Fréquence du réseau de référence");
            case "wifi_count": return p("Redes informadas no cache", "Networks reported in cache", "Redes reportadas en caché", "Réseaux signalés dans le cache");
            case "adapter_enabled": return p("Adaptador habilitado", "Adapter enabled", "Adaptador habilitado", "Adaptateur activé");
            case "elapsed_time": return p("Tempo desde a inicialização", "Time since boot", "Tiempo desde el arranque", "Temps depuis le démarrage");
            case "servicemode_label": return p("ServiceMode: sem API pública", "ServiceMode: no public API", "ServiceMode: sin API pública", "ServiceMode : aucune API publique");
            case "tower_label": return p("Coordenadas de torres: base não conectada", "Tower coordinates: database not connected", "Coordenadas de torres: base no conectada", "Coordonnées des antennes : base non connectée");
            case "not_public": return p("Sem API pública", "No public API", "Sin API pública", "Aucune API publique");
            case "not_connected": return p("Base não conectada", "Database not connected", "Base no conectada", "Base non connectée");
            case "session": return p("Sessão", "Session", "Sesión", "Session");
            case "platform": return p("Plataforma", "Platform", "Plataforma", "Plateforme");
            case "unsupported_count": return p("Sensores protegidos ou não amostrados", "Protected or unsampled sensors", "Sensores protegidos o no muestreados", "Capteurs protégés ou non échantillonnés");
            case "omitted_count": return p("Sensores omitidos pelo limite do catálogo", "Sensors omitted by catalogue limit", "Sensores omitidos por el límite del catálogo", "Capteurs omis par la limite du catalogue");
            case "public_limits": return p("Somente APIs públicas; sem varredura ativa, ServiceMode privado ou base externa de torres.", "Public APIs only; no active scan, private ServiceMode or external tower database.", "Solo API públicas; sin barrido activo, ServiceMode privado ni base externa de torres.", "API publiques uniquement ; aucun balayage actif, ServiceMode privé ou base externe d'antennes.");
            default: return key;
        }
    }

    public String state(String token) {
        if (token == null) return t("not_available");
        if (token.contains("RUNNING") || token.contains("OBSERVING"))
            return p("observação ativa", "observation active", "observación activa", "observation active");
        if (token.contains("STOPPED") || token.contains("NOT_RUN"))
            return p("parado", "stopped", "detenido", "arrêté");
        if (token.contains("NOT_PUBLIC_API")) return t("not_public");
        if (token.contains("NOT_CONNECTED")) return t("not_connected");
        if (token.contains("PERMISSION")) return p("permissão necessária", "permission required", "permiso necesario", "autorisation requise");
        if (token.contains("UNAVAILABLE") || token.contains("NOT_PRESENT") || token.contains("NOT_OBSERVED")) return t("not_available");
        return p("estado técnico registrado na exportação", "technical state recorded in export", "estado técnico registrado en la exportación", "état technique enregistré dans l'exportation");
    }
}
