"use client";

import Link from "next/link";
import { useState, useEffect, FormEvent } from "react";
import { doc, getDoc, collection, addDoc } from "firebase/firestore";
import { db } from "@/lib/firebase";

export default function Home() {
    const [heroData, setHeroData] = useState({
        title: "",
        subtitle: "",
        year: "",
        backgroundImage: "",
        whatsapp: ""
    });

    const [events, setEvents] = useState<string[]>([]);

    const [offeringConfig, setOfferingConfig] = useState({
        message: "",
        alias: "",
        cbu: ""
    });

    // Radio Config
    const [radioData, setRadioData] = useState({
        qrImage: "https://lh3.googleusercontent.com/aida-public/AB6AXuAYWtL7z9oEpxB7hg6XzrdVqcMh6ypceshnp-oeYWrZidjFQ5znpy08zGLAqMncMhZWnP0qQQJ5DDhLoogbr01QjhYonS0UoPpfUkYOhPRPzn8AkU6g3-3hHjXG6FlJL1pT-67RSyEk1YjC-LpPuTuVTMkO5WiCLh7CqqAZMJTcfAuRNG6AzySTa12Pk3fC0Av6aZ9hEzoqezFpWabVvNmYC7uHS-5NKS_sHoRKwh7moHM59-NiBEWmzXdGqebBuavjK0KDj0bdNmQ",
        playStoreLink: "https://play.google.com/store/apps/details?id=com.radiocristiana"
    });

    const [socialConfig, setSocialConfig] = useState({
        facebookUrl: "",
        instagramUrl: ""
    });

    // Loading State
    const [loading, setLoading] = useState(true);

    // Modal States
    const [isPrayerModalOpen, setIsPrayerModalOpen] = useState(false);
    const [isOfferingModalOpen, setIsOfferingModalOpen] = useState(false);
    const [isMobileMenuOpen, setIsMobileMenuOpen] = useState(false);

    // Prayer Form State
    const [prayerForm, setPrayerForm] = useState({
        name: "",
        phone: "",
        request: ""
    });
    const [isSubmitting, setIsSubmitting] = useState(false);

    useEffect(() => {
        const fetchContent = async () => {
            try {
                // Fetch Landing Page Content
                const landingRef = doc(db, "content", "landing");
                const landingSnap = await getDoc(landingRef);
                if (landingSnap.exists()) {
                    const data = landingSnap.data();
                    if (data.hero) setHeroData(prev => ({ ...prev, ...data.hero }));
                    if (data.events && Array.isArray(data.events)) setEvents(data.events);
                    if (data.offering) setOfferingConfig(prev => ({ ...prev, ...data.offering }));
                    if (data.radio) setRadioData(prev => ({ ...prev, ...data.radio }));
                }

                // Fetch Global Config (Social Media)
                const configRef = doc(db, "settings", "about");
                const configSnap = await getDoc(configRef);
                if (configSnap.exists()) {
                    const data = configSnap.data();
                    setSocialConfig({
                        facebookUrl: data.facebookUrl || "https://www.facebook.com",
                        instagramUrl: data.instagramUrl || "https://www.instagram.com"
                    });
                }
            } catch (error) {
                console.error("Error fetching content:", error);
            } finally {
                setLoading(false);
            }
        };

        fetchContent();
    }, []);

    const handlePrayerSubmit = async (e: FormEvent) => {
        e.preventDefault();
        setIsSubmitting(true);

        try {
            await addDoc(collection(db, "prayer_requests"), {
                ...prayerForm,
                timestamp: new Date()
            });

            alert("¡Tu pedido de oración ha sido enviado!");
            setIsPrayerModalOpen(false);
            setPrayerForm({ name: "", phone: "", request: "" });

        } catch (error) {
            console.error("Error enviando pedido:", error);
            alert("Hubo un error al enviar tu pedido. Por favor intenta nuevamente.");
        } finally {
            setIsSubmitting(false);
        }
    };

    if (loading) {
        return (
            <div className="min-h-screen bg-black flex items-center justify-center">
                <div className="animate-spin rounded-full h-16 w-16 border-t-2 border-b-2 border-white"></div>
            </div>
        );
    }

    return (
        <div className="min-h-screen">
            {/* Top Bar */}
            <div className="bg-black text-white py-2 px-4 text-xs font-display border-b border-gray-800">
                <div className="max-w-7xl mx-auto flex justify-between items-center">
                    <div className="flex items-center space-x-4">
                        <span className="flex items-center gap-1">
                            <span className="material-symbols-outlined text-sm">language</span> ES
                        </span>
                        <span className="hidden md:flex items-center gap-1">
                            <i className="fa-brands fa-whatsapp text-sm mr-1"></i>
                            {heroData.whatsapp}
                        </span>
                    </div>
                    <div className="flex items-center space-x-4">
                        <span className="text-gray-300 transition-colors">
                            contacto@ccbes.com.ar
                        </span>
                        <div className="flex space-x-3 text-sm">
                            <a className="hover:text-gray-400 transition-colors" href={socialConfig.facebookUrl} target="_blank" rel="noopener noreferrer">
                                <i className="fa-brands fa-facebook-f"></i>
                            </a>
                            <a className="hover:text-gray-400 transition-colors" href={socialConfig.instagramUrl} target="_blank" rel="noopener noreferrer">
                                <i className="fa-brands fa-instagram"></i>
                            </a>
                        </div>
                    </div>
                </div>
            </div>

            {/* Navigation */}
            <nav className="sticky top-0 z-50 bg-white/90 dark:bg-black/90 backdrop-blur-md shadow-sm">
                <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
                    <div className="flex justify-between items-center h-20">
                        <div className="flex-shrink-0 flex items-center">
                            <div className="flex items-center gap-3">
                                <img alt="CCBES Logo" className="w-12 h-12 object-contain" src="logo.png" />
                                <div className="flex flex-col leading-none">
                                    <span className="font-display font-bold text-sm tracking-tight uppercase text-black dark:text-white">
                                        Centro Cristiano
                                    </span>
                                    <span className="font-display font-bold text-sm tracking-tight uppercase text-black dark:text-white">
                                        Bienvenido Espiritu Santo
                                    </span>
                                </div>
                            </div>
                        </div>
                        <div className="hidden md:block">
                            <div className="ml-10 flex items-baseline space-x-8 font-display text-sm font-semibold tracking-wide uppercase">
                                <a className="hover:text-gray-500 transition-colors" href="#">Inicio</a>
                                <a className="hover:text-gray-500 transition-colors" href="#eventos">Eventos</a>
                                <a className="hover:text-gray-500 transition-colors" href="#radio">Radio CCBES</a>
                                <button onClick={() => setIsOfferingModalOpen(true)} className="hover:text-gray-500 transition-colors text-left uppercase">Ofrendar</button>
                                <a className="hover:text-gray-500 transition-colors" href="#sobre-nosotros">Sobre Nosotros</a>
                            </div>
                        </div>
                        <div className="md:hidden">
                            <button
                                onClick={() => setIsMobileMenuOpen(!isMobileMenuOpen)}
                                className="p-2 rounded-md text-gray-600 dark:text-gray-400 hover:bg-gray-100 dark:hover:bg-gray-800 transition"
                            >
                                <span className="material-symbols-outlined">{isMobileMenuOpen ? 'close' : 'menu'}</span>
                            </button>
                        </div>
                    </div>
                </div>

                {/* Mobile Menu Overlay */}
                {isMobileMenuOpen && (
                    <div className="md:hidden absolute top-20 left-0 w-full bg-white dark:bg-black border-t dark:border-gray-800 shadow-xl p-4 flex flex-col space-y-4 font-display font-bold uppercase tracking-wide animate-in slide-in-from-top-4 duration-200">
                        <a onClick={() => setIsMobileMenuOpen(false)} className="block py-2 px-4 hover:bg-gray-50 dark:hover:bg-gray-900 rounded-lg" href="#">Inicio</a>
                        <a onClick={() => setIsMobileMenuOpen(false)} className="block py-2 px-4 hover:bg-gray-50 dark:hover:bg-gray-900 rounded-lg" href="#eventos">Eventos</a>
                        <a onClick={() => setIsMobileMenuOpen(false)} className="block py-2 px-4 hover:bg-gray-50 dark:hover:bg-gray-900 rounded-lg" href="#radio">Radio CCBES</a>
                        <button onClick={() => { setIsOfferingModalOpen(true); setIsMobileMenuOpen(false); }} className="block w-full text-left py-2 px-4 hover:bg-gray-50 dark:hover:bg-gray-900 rounded-lg uppercase">Ofrendar</button>
                        <a onClick={() => setIsMobileMenuOpen(false)} className="block py-2 px-4 hover:bg-gray-50 dark:hover:bg-gray-900 rounded-lg" href="#sobre-nosotros">Sobre Nosotros</a>
                    </div>
                )}
            </nav>

            {/* Hero Section */}
            <header
                className="relative h-[85vh] min-h-[700px] flex items-center justify-center text-white overflow-hidden"
                style={{
                    backgroundImage: `url(${heroData.backgroundImage})`,
                    backgroundSize: 'cover',
                    backgroundPosition: 'center'
                }}
            >
                {/* Overlay gradient for readability */}
                <div className="absolute inset-0 bg-black/40"></div>

                <div className="relative z-10 max-w-7xl mx-auto px-4 text-center flex flex-col items-center">
                    {/* Header (Top) */}
                    <div className="mb-6">
                        <div className="border-[3px] border-white rounded-full px-8 py-2 text-3xl font-black tracking-tighter">
                            {heroData.year}
                        </div>
                    </div>
                    {/* Main Title */}
                    <h1 className="text-6xl md:text-9xl font-display font-black tracking-tight uppercase mb-4 leading-none text-shadow-lg whitespace-pre-line text-white">
                        {heroData.title}
                    </h1>
                    {/* Subtitle */}
                    <p className="text-2xl md:text-4xl font-display font-bold uppercase tracking-[0.2em] mb-12 border-y-2 border-white/50 inline-block py-3 px-8 text-white">
                        {heroData.subtitle}
                    </p>
                </div>
            </header>

            <main className="max-w-7xl mx-auto px-4 py-16 space-y-8">
                {/* Radio Section */}
                <section
                    className="bg-white dark:bg-gray-900 rounded-xl shadow-lg overflow-hidden flex flex-col md:flex-row items-center"
                    id="radio"
                >
                    <div className="p-8 md:p-12 flex-1">
                        <h2 className="text-4xl font-display font-light mb-6">
                            Radio <span className="font-bold">CCBES</span>
                        </h2>
                        <p className="text-gray-600 dark:text-gray-400 mb-0 max-w-lg leading-relaxed text-lg">
                            Sintonizanos en cualquier momento y lugar a través de nuestra aplicación oficial. Disfrutá de la mejor
                            programación cristiana directamente en tu dispositivo móvil.
                        </p>
                    </div>
                    <div className="bg-gray-50 dark:bg-gray-800/50 p-12 flex flex-col items-center justify-center border-l dark:border-gray-800">
                        <div className="bg-white p-6 rounded-xl shadow-inner mb-4">
                            <img
                                alt="QR Code Radio CCBES"
                                className="w-48 h-48 object-contain"
                                src={radioData.qrImage}
                            />
                        </div>
                        <p className="text-sm uppercase font-black text-gray-700 dark:text-gray-300 tracking-widest text-center mb-4">
                            ESCANEA EL CÓDIGO QR
                        </p>
                        <a
                            href={radioData.playStoreLink}
                            target="_blank"
                            rel="noopener noreferrer"
                            className="inline-block transition-transform hover:scale-105"
                        >
                            <img
                                src="https://upload.wikimedia.org/wikipedia/commons/7/78/Google_Play_Store_badge_EN.svg"
                                alt="Disponible en Google Play"
                                className="h-14"
                            />
                        </a>
                    </div>
                </section>

                {/* Prayer & Offering Grid */}
                <div className="grid grid-cols-1 md:grid-cols-2 gap-8">
                    <div className="bg-white dark:bg-gray-900 p-10 rounded-xl shadow-lg border-t-4 border-primary">
                        <h3 className="text-3xl font-display font-light mb-4">
                            ¿Tenés un pedido<br />
                            <span className="font-bold">de Oración?</span>
                        </h3>
                        <p className="text-gray-600 dark:text-gray-400 mb-8 leading-relaxed">
                            Si tenés un pedido de oración, estaremos intercediendo por vos.
                        </p>
                        <button
                            onClick={() => setIsPrayerModalOpen(true)}
                            className="w-full bg-primary text-white py-4 font-bold uppercase tracking-wider rounded-lg hover:opacity-90 transition cursor-pointer"
                        >
                            Oren por mi necesidad
                        </button>
                    </div>
                    <div className="bg-white dark:bg-gray-900 p-10 rounded-xl shadow-lg border-t-4 border-primary" id="ofrendar">
                        <h3 className="text-3xl font-display font-light mb-4">Ofrendar</h3>
                        <p className="text-gray-600 dark:text-gray-400 mb-8 leading-relaxed">
                            Gracias por tu deseo de contribuir con la misión que Dios nos encomendó como Iglesia.
                        </p>
                        <button
                            onClick={() => setIsOfferingModalOpen(true)}
                            className="w-full bg-primary text-white py-4 font-bold uppercase tracking-wider rounded-lg hover:opacity-90 transition cursor-pointer"
                        >
                            Quiero Ofrendar
                        </button>
                    </div>
                </div>

                {/* Events Section */}
                <section className="py-12" id="eventos">
                    <div className="flex justify-between items-end mb-8">
                        <div>
                            <h2 className="text-4xl font-display font-bold uppercase tracking-tight">Eventos de la Iglesia</h2>
                            <p className="text-gray-500">Actividades, conferencias y momentos compartidos.</p>
                        </div>
                    </div>
                    <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                        {events.map((url, index) => (
                            <div key={index} className="aspect-square rounded-xl overflow-hidden shadow-md group hover:shadow-xl transition-all duration-300">
                                <img
                                    alt={`Evento ${index + 1}`}
                                    className="w-full h-full object-cover group-hover:scale-105 transition-transform duration-500"
                                    src={url}
                                    loading="lazy"
                                />
                            </div>
                        ))}
                    </div>
                </section>

                {/* About Section */}
                <section className="py-12 border-t border-gray-200 dark:border-gray-800" id="sobre-nosotros">
                    <div className="bg-white dark:bg-gray-900 p-8 md:p-12 rounded-xl shadow-md w-full">
                        <h2 className="text-4xl font-display font-light mb-8 text-black dark:text-white">
                            Reuniones<br />
                            <span className="font-bold uppercase">Presenciales</span>
                        </h2>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-8 mb-12 max-w-2xl">
                            <div className="space-y-2">
                                <p className="font-bold text-primary dark:text-white text-xl uppercase tracking-wider">Jueves</p>
                                <p className="text-lg">19:30 Hs.</p>
                            </div>
                            <div className="space-y-2">
                                <p className="font-bold text-primary dark:text-white text-xl uppercase tracking-wider">Sábados</p>
                                <p className="text-lg">19:30 Hs.</p>
                            </div>
                        </div>
                        <div className="p-6 bg-gray-100 dark:bg-gray-800 rounded-lg">
                            <div className="flex items-start gap-4 text-black dark:text-white">
                                <span className="material-symbols-outlined mt-1">location_on</span>
                                <div>
                                    <p className="font-bold">Calle 431 & calle 54</p>
                                    <p className="text-gray-500">Mar del Plata, Argentina</p>
                                </div>
                            </div>
                        </div>
                    </div>
                </section>

                {/* Map Section */}
                <section className="h-[400px] rounded-xl overflow-hidden shadow-lg border border-gray-200 dark:border-gray-800">
                    <iframe
                        allowFullScreen
                        height="100%"
                        loading="lazy"
                        src="https://www.google.com/maps/embed?pb=!1m18!1m12!1m3!1d3137.989710332353!2d-57.6406979!3d-38.1404111!2m3!1f0!2f0!3f0!3m2!1i1024!2i768!4f13.1!3m3!1m2!1s0x9584e60f081d4519%3A0xc3c9402e1c9e8310!2sCalle%20431%20%26%20Calle%2054%2C%20B7600%20Mar%20del%20Plata%2C%20Provincia%20de%20Buenos%20Aires!5e0!3m2!1ses!2sar!4v1715600000000!5m2!1ses!2sar"
                        style={{ border: 0 }}
                        width="100%"
                    ></iframe>
                </section>
            </main>

            {/* Footer */}
            <footer className="bg-black text-white py-12 px-4 border-t border-gray-800 mt-20">
                <div className="max-w-7xl mx-auto flex flex-col md:flex-row justify-between items-center gap-8">
                    <div className="flex items-center gap-4">
                        <img alt="CCBES Logo" className="w-14 h-14 object-contain" src="logo.png" />
                        <div className="text-left">
                            <div className="flex flex-col">
                                <h4 className="font-display font-bold uppercase tracking-tighter text-lg leading-tight">
                                    Centro Cristiano
                                </h4>
                                <h4 className="font-display font-bold uppercase tracking-tighter text-lg leading-tight">
                                    Bienvenido Espiritu Santo
                                </h4>
                            </div>
                            <p className="text-[10px] text-gray-500 uppercase tracking-widest mt-1">
                                © 2025 CCBES. Mar del Plata, Argentina.
                            </p>
                        </div>
                    </div>
                    <div className="flex space-x-6 items-center">
                        <a className="text-gray-400 hover:text-white transition-colors text-xl" href={socialConfig.facebookUrl} target="_blank" rel="noopener noreferrer">
                            <i className="fa-brands fa-facebook-f"></i>
                        </a>
                        <a className="text-gray-400 hover:text-white transition-colors text-xl" href={socialConfig.instagramUrl} target="_blank" rel="noopener noreferrer">
                            <i className="fa-brands fa-instagram"></i>
                        </a>
                    </div>
                </div>
            </footer>

            {/* Prayer Modal */}
            {isPrayerModalOpen && (
                <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200">
                    <div className="bg-white dark:bg-gray-900 w-full max-w-md rounded-2xl p-6 shadow-2xl relative">
                        <button
                            onClick={() => setIsPrayerModalOpen(false)}
                            className="absolute top-4 right-4 text-gray-400 hover:text-gray-600 transition"
                        >
                            <span className="material-symbols-outlined">close</span>
                        </button>
                        <h3 className="text-2xl font-display font-bold mb-4 text-center">
                            Pedido de Oración
                        </h3>
                        <p className="text-gray-500 mb-6 text-center text-sm">
                            Compartinos tu necesidad, estaremos orando por vos.
                        </p>

                        <form onSubmit={handlePrayerSubmit} className="space-y-4">
                            <div>
                                <label className="block text-sm font-bold mb-1">Nombre</label>
                                <input
                                    type="text"
                                    required
                                    value={prayerForm.name}
                                    onChange={e => setPrayerForm({ ...prayerForm, name: e.target.value })}
                                    className="w-full p-3 rounded-lg border border-gray-300 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 focus:ring-2 focus:ring-primary outline-none transition"
                                    placeholder="Tu nombre completo"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-bold mb-1">Teléfono</label>
                                <input
                                    type="tel"
                                    required
                                    value={prayerForm.phone}
                                    onChange={e => setPrayerForm({ ...prayerForm, phone: e.target.value })}
                                    className="w-full p-3 rounded-lg border border-gray-300 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 focus:ring-2 focus:ring-primary outline-none transition"
                                    placeholder="Ej: +54 9 11..."
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-bold mb-1">Tu Pedido</label>
                                <textarea
                                    required
                                    value={prayerForm.request}
                                    onChange={e => setPrayerForm({ ...prayerForm, request: e.target.value })}
                                    className="w-full p-3 rounded-lg border border-gray-300 dark:border-gray-700 bg-gray-50 dark:bg-gray-800 focus:ring-2 focus:ring-primary outline-none transition min-h-[100px]"
                                    placeholder="Escribinos tu petición..."
                                ></textarea>
                            </div>

                            <button
                                type="submit"
                                disabled={isSubmitting}
                                className="w-full py-4 font-bold uppercase rounded-lg bg-primary text-white hover:opacity-90 transition disabled:opacity-50 mt-2"
                            >
                                {isSubmitting ? 'Enviando...' : 'Enviar Pedido'}
                            </button>
                        </form>
                    </div>
                </div>
            )}

            {/* Offering Modal */}
            {isOfferingModalOpen && (
                <div className="fixed inset-0 z-[100] flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in duration-200">
                    <div className="bg-white dark:bg-gray-900 w-full max-w-md rounded-2xl p-8 shadow-2xl relative">
                        <button
                            onClick={() => setIsOfferingModalOpen(false)}
                            className="absolute top-4 right-4 text-gray-400 hover:text-gray-600 transition"
                        >
                            <span className="material-symbols-outlined">close</span>
                        </button>
                        <h3 className="text-2xl font-display font-bold mb-6 text-center text-primary">
                            Ofrendar
                        </h3>

                        <div className="text-center space-y-6">
                            <p className="text-gray-600 dark:text-gray-300 font-medium italic text-lg leading-relaxed">
                                "{offeringConfig.message}"
                            </p>

                            <hr className="border-gray-200 dark:border-gray-800" />

                            <div className="space-y-4">
                                <div className="space-y-1">
                                    <p className="text-xs font-bold uppercase tracking-widest text-gray-400">ALIAS</p>
                                    <div className="bg-gray-50 dark:bg-gray-800 p-3 rounded-lg border border-gray-200 dark:border-gray-700 flex justify-between items-center group">
                                        <span className="font-mono text-lg font-bold select-all">{offeringConfig.alias}</span>
                                        <button
                                            onClick={() => navigator.clipboard.writeText(offeringConfig.alias)}
                                            className="text-gray-400 hover:text-primary transition"
                                            title="Copiar Alias"
                                        >
                                            <span className="material-symbols-outlined text-sm">content_copy</span>
                                        </button>
                                    </div>
                                </div>

                                <div className="space-y-1">
                                    <p className="text-xs font-bold uppercase tracking-widest text-gray-400">CBU / CVU</p>
                                    <div className="bg-gray-50 dark:bg-gray-800 p-3 rounded-lg border border-gray-200 dark:border-gray-700 flex justify-between items-center group">
                                        <span className="font-mono text-sm font-bold select-all break-all">{offeringConfig.cbu}</span>
                                        <button
                                            onClick={() => navigator.clipboard.writeText(offeringConfig.cbu)}
                                            className="text-gray-400 hover:text-primary transition"
                                            title="Copiar CBU"
                                        >
                                            <span className="material-symbols-outlined text-sm">content_copy</span>
                                        </button>
                                    </div>
                                </div>
                            </div>

                            <button
                                onClick={() => setIsOfferingModalOpen(false)}
                                className="w-full py-4 font-bold uppercase rounded-lg border border-gray-200 hover:bg-gray-50 transition text-gray-600 mt-4"
                            >
                                Cerrar
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
