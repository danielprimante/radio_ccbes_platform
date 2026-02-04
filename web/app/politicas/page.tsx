"use client";

import Link from "next/link";

export default function PoliticasPrivacidad() {
    return (
        <div className="min-h-screen bg-background-light dark:bg-background-dark font-body text-gray-900 dark:text-gray-100">
            {/* Header / Banner */}
            <header className="bg-black text-white py-16 px-4 border-b border-gray-800">
                <div className="max-w-4xl mx-auto text-center">
                    <img alt="CCBES Logo" className="w-20 h-20 object-contain mx-auto mb-6" src="/logo.png" />
                    <h1 className="text-4xl md:text-5xl font-display font-black tracking-tight uppercase mb-4">
                        Políticas de Privacidad
                    </h1>
                    <p className="text-xl text-gray-400 font-display font-medium uppercase tracking-widest">
                        Radio CCBES & CCBES Mar del Plata
                    </p>
                </div>
            </header>

            {/* Content Section */}
            <main className="max-w-4xl mx-auto px-4 py-20">
                <div className="prose prose-lg dark:prose-invert max-w-none space-y-12">
                    
                    <section>
                        <h2 className="text-2xl font-display font-bold uppercase border-b-2 border-primary dark:border-white pb-2 mb-6 text-primary dark:text-white">
                            1. Introducción
                        </h2>
                        <p className="leading-relaxed">
                            En <strong>Radio CCBES</strong> y el <strong>Centro Cristiano Bienvenido Espíritu Santo (CCBES)</strong>, valoramos su privacidad y estamos comprometidos a proteger sus datos personales. Esta política de privacidad explica cómo recopilamos, usamos y protegemos la información cuando utiliza nuestra aplicación móvil y nuestro sitio web.
                        </p>
                    </section>

                    <section>
                        <h2 className="text-2xl font-display font-bold uppercase border-b-2 border-primary dark:border-white pb-2 mb-6 text-primary dark:text-white">
                            2. Información que Recopilamos
                        </h2>
                        <p className="leading-relaxed mb-4">
                            Recopilamos información limitada para proporcionar y mejorar nuestros servicios:
                        </p>
                        <ul className="list-disc pl-6 space-y-2">
                            <li><strong>Información de contacto:</strong> Nombre y teléfono proporcionados voluntariamente en solicitudes de oración.</li>
                            <li><strong>Datos de uso:</strong> Información sobre cómo utiliza la aplicación para mejorar la transmisión de audio.</li>
                            <li><strong>Identificadores de dispositivo:</strong> Podríamos recopilar identificadores únicos para el envío de notificaciones push a través de servicios como OneSignal.</li>
                        </ul>
                    </section>

                    <section>
                        <h2 className="text-2xl font-display font-bold uppercase border-b-2 border-primary dark:border-white pb-2 mb-6 text-primary dark:text-white">
                            3. Uso de la Información
                        </h2>
                        <p className="leading-relaxed">
                            Utilizamos los datos recopilados para:
                        </p>
                        <ul className="list-disc pl-6 space-y-2 mt-4">
                            <li>Gestionar y responder a sus pedidos de oración.</li>
                            <li>Enviar notificaciones sobre eventos, programas de radio o noticias importantes.</li>
                            <li>Garantizar el correcto funcionamiento de la transmisión de radio en vivo.</li>
                            <li>Mantener la seguridad y prevenir el fraude en nuestra plataforma.</li>
                        </ul>
                    </section>

                    <section className="bg-gray-100 dark:bg-gray-800/50 p-8 rounded-2xl border-l-4 border-primary dark:border-white">
                        <h2 className="text-2xl font-display font-bold uppercase mb-4 text-primary dark:text-white">
                            4. Privacidad de Menores
                        </h2>
                        <p className="leading-relaxed italic">
                            Nuestra plataforma está dirigida a un público general, incluyendo familias. No recopilamos intencionadamente información personal de niños menores de 13 años (o la edad mínima legal en su jurisdicción) sin el consentimiento previo y verificable de sus padres o tutores legales. Si tomamos conocimiento de que hemos recopilado inadvertidamente información de un menor sin dicho consentimiento, eliminaremos esa información de nuestros registros de inmediato.
                        </p>
                    </section>

                    <section>
                        <h2 className="text-2xl font-display font-bold uppercase border-b-2 border-primary dark:border-white pb-2 mb-6 text-primary dark:text-white">
                            5. Datos Sensibles y Permisos
                        </h2>
                        <p className="leading-relaxed">
                            Respetamos rigurosamente las políticas de la Play Store respecto a datos sensibles. Nuestra aplicación:
                        </p>
                        <ul className="list-disc pl-6 space-y-2 mt-4">
                            <li>No accede a sus contactos ni registros de llamadas sin permiso explícito.</li>
                            <li>No recopila información de ubicación precisa en segundo plano.</li>
                            <li>Utiliza cifrado estándar para proteger cualquier transmisión de datos personales.</li>
                        </ul>
                    </section>

                    <section>
                        <h2 className="text-2xl font-display font-bold uppercase border-b-2 border-primary dark:border-white pb-2 mb-6 text-primary dark:text-white">
                            6. Terceros
                        </h2>
                        <p className="leading-relaxed">
                            Podemos compartir datos limitados con proveedores de servicios esenciales como Firebase (para la base de datos) y OneSignal (para notificaciones). Estos proveedores están sujetos a sus propias políticas de privacidad y se comprometen a proteger sus datos.
                        </p>
                    </section>

                    <section>
                        <h2 className="text-2xl font-display font-bold uppercase border-b-2 border-primary dark:border-white pb-2 mb-6 text-primary dark:text-white">
                            7. Sus Derechos
                        </h2>
                        <p className="leading-relaxed">
                            Usted tiene derecho a acceder, rectificar o solicitar la eliminación de sus datos personales en cualquier momento. Para ejercer estos derechos, puede contactarnos a través de los canales oficiales proporcionados en el sitio.
                        </p>
                    </section>

                    <section>
                        <h2 className="text-2xl font-display font-bold uppercase border-b-2 border-primary dark:border-white pb-2 mb-6 text-primary dark:text-white">
                            8. Contacto
                        </h2>
                        <p className="leading-relaxed">
                            Si tiene preguntas sobre esta política, contáctenos en:<br />
                            <strong>Email:</strong> contacto@ccbes.com.ar<br />
                            <strong>Ubicación:</strong> Calle 431 & calle 54, Mar del Plata, Argentina.
                        </p>
                    </section>

                    <div className="text-sm text-gray-500 pt-10 text-center">
                        Última actualización: 17 de enero de 2026
                    </div>
                </div>
            </main>

            {/* Footer */}
            <footer className="bg-black text-white py-12 px-4 border-t border-gray-800">
                <div className="max-w-4xl mx-auto flex flex-col md:flex-row justify-between items-center gap-6">
                    <div className="flex items-center gap-4">
                        <img alt="CCBES Logo" className="w-12 h-12 object-contain" src="/logo.png" />
                        <span className="font-display font-bold uppercase text-xs tracking-tighter">
                            CCBES Mar del Plata
                        </span>
                    </div>
                    <p className="text-[10px] text-gray-500 uppercase tracking-widest">
                        © 2026 CCBES. Todos los derechos reservados.
                    </p>
                </div>
            </footer>
        </div>
    );
}
