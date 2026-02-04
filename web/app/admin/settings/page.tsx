'use client';

import { useEffect, useState, useRef } from 'react';
import { useRouter } from 'next/navigation';
import { onAuthStateChanged } from 'firebase/auth';
import { auth } from '@/lib/firebase';
import { getRadioSettings, updateRadioSettings, getAboutSettings, updateAboutSettings, RadioSettings, AboutSettings } from '@/lib/api';
import AdminGuard from '@/components/AdminGuard';
import Link from 'next/link';
import ImageUploader from '@/components/ImageUploader';

const DEFAULT_STREAM_URL = "https://streaming01.shockmedia.com.ar:10586/stream";
const DEFAULT_ABOUT_SETTINGS = {
    churchName: "Iglesia CCBES",
    subText: "Centro Cristiano Bienvenido Espiritu Santo",
    location: "Calle 431, Mar del Plata, Argentina",
    email: "contacto@ccbes.org",
    phone: "+54 9 223 123-4567"
};

export default function SettingsPage() {
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [hasChanges, setHasChanges] = useState(false);
    const [message, setMessage] = useState<{ type: 'success' | 'error', text: string } | null>(null);

    // Form inputs
    const [radioConfig, setRadioConfig] = useState<RadioSettings>({
        streamUrl: '',
        title: '',
        subtitle: ''
    });

    const [aboutConfig, setAboutConfig] = useState<AboutSettings>({
        logoUrl: '',
        churchName: '',
        subText: '',
        location: '',
        email: '',
        phone: '',
        facebookUrl: '',
        instagramUrl: '',
        youtubeUrl: ''
    });

    useEffect(() => {
        const loadInitialData = async () => {
            try {
                console.log('Cargando configuración desde Firestore...');
                const [radioData, aboutData] = await Promise.all([
                    getRadioSettings(),
                    getAboutSettings()
                ]);

                if (radioData) {
                    setRadioConfig({
                        streamUrl: radioData.streamUrl || DEFAULT_STREAM_URL,
                        title: radioData.title || '',
                        subtitle: radioData.subtitle || ''
                    });
                } else {
                    setRadioConfig(prev => ({ ...prev, streamUrl: DEFAULT_STREAM_URL }));
                }

                if (aboutData) {
                    setAboutConfig({
                        logoUrl: aboutData.logoUrl || '',
                        churchName: aboutData.churchName || DEFAULT_ABOUT_SETTINGS.churchName,
                        subText: aboutData.subText || DEFAULT_ABOUT_SETTINGS.subText,
                        location: aboutData.location || DEFAULT_ABOUT_SETTINGS.location,
                        email: aboutData.email || DEFAULT_ABOUT_SETTINGS.email,
                        phone: aboutData.phone || DEFAULT_ABOUT_SETTINGS.phone,
                        facebookUrl: aboutData.facebookUrl || '',
                        instagramUrl: aboutData.instagramUrl || '',
                        youtubeUrl: aboutData.youtubeUrl || ''
                    });
                } else {
                    setAboutConfig(prev => ({
                        ...prev,
                        churchName: DEFAULT_ABOUT_SETTINGS.churchName,
                        subText: DEFAULT_ABOUT_SETTINGS.subText,
                        location: DEFAULT_ABOUT_SETTINGS.location,
                        email: DEFAULT_ABOUT_SETTINGS.email,
                        phone: DEFAULT_ABOUT_SETTINGS.phone
                    }));
                }
            } catch (error) {
                console.error('Error loading settings:', error);
                setMessage({ type: 'error', text: 'Error al cargar la configuración' });
            } finally {
                setLoading(false);
            }
        };

        loadInitialData();
    }, []);

    const handleChange = (configType: 'radio' | 'about', updates: any) => {
        setHasChanges(true);
        if (configType === 'radio') {
            setRadioConfig(prev => ({ ...prev, ...updates }));
        } else {
            setAboutConfig(prev => {
                const newData = { ...prev, ...updates };
                console.log('Actualizando aboutConfig localmente:', newData);
                return newData;
            });
        }
    };

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        setSaving(true);
        setMessage(null);

        try {
            console.log('Guardando configuración completa en Firestore...');
            console.log('Datos a guardar (About):', aboutConfig);

            await Promise.all([
                updateRadioSettings(radioConfig),
                updateAboutSettings(aboutConfig)
            ]);

            setHasChanges(false);
            setMessage({ type: 'success', text: 'Configuración guardada correctamente en el servidor' });

            // Forzar una recarga visual de los datos para confirmar
            const freshData = await getAboutSettings();
            console.log('Confirmación - Datos persistidos en servidor:', freshData);
            if (freshData) setAboutConfig(prev => ({ ...prev, ...freshData }));

        } catch (error) {
            console.error('Error saving settings:', error);
            setMessage({ type: 'error', text: 'Error fatal al guardar en el servidor' });
        } finally {
            setSaving(false);
        }
    };

    // Logo upload is now handled by ImageUploader component

    if (loading) {
        return (
            <div className="min-h-[60vh] flex items-center justify-center">
                <div className="text-gray-400 animate-pulse font-medium">Cargando configuración...</div>
            </div>
        );
    }

    return (
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            <div className="max-w-4xl mx-auto">
                <form onSubmit={handleSubmit} className="space-y-6">
                    {hasChanges && (
                        <div className="bg-amber-50 border border-amber-200 p-4 rounded-lg flex items-center justify-between sticky top-4 z-10 shadow-md">
                            <div className="flex items-center gap-3">
                                <div className="w-2 h-2 bg-amber-500 rounded-full animate-pulse"></div>
                                <p className="text-amber-800 font-medium">Tienes cambios sin guardar</p>
                            </div>
                            <button
                                type="submit"
                                className="bg-amber-600 text-white px-4 py-1.5 rounded-md text-sm font-bold hover:bg-amber-700 transition-colors"
                            >
                                Guardar Ahora
                            </button>
                        </div>
                    )}

                    {message && (
                        <div className={`p-4 rounded-md shadow-sm border ${message.type === 'success' ? 'bg-green-50 text-green-700 border-green-200' : 'bg-red-50 text-red-700 border-red-200'}`}>
                            {message.text}
                        </div>
                    )}

                    {/* Radio Config */}
                    <div className="bg-white rounded-lg shadow overflow-hidden">
                        <div className="px-6 py-4 border-b border-gray-200">
                            <h2 className="text-lg font-semibold text-gray-900">Radio y Reproductor</h2>
                        </div>
                        <div className="p-6 space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-gray-700">URL del Stream</label>
                                <input
                                    type="url"
                                    required
                                    value={radioConfig.streamUrl}
                                    onChange={(e) => handleChange('radio', { streamUrl: e.target.value })}
                                    className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-red-500 focus:ring-red-500 border p-2"
                                />
                            </div>
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700">Título Principal</label>
                                    <input
                                        type="text"
                                        value={radioConfig.title}
                                        onChange={(e) => handleChange('radio', { title: e.target.value })}
                                        className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-red-500 focus:ring-red-500 border p-2"
                                        placeholder="La Mejor Compañía"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-700">Subtítulo</label>
                                    <input
                                        type="text"
                                        value={radioConfig.subtitle}
                                        onChange={(e) => handleChange('radio', { subtitle: e.target.value })}
                                        className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-red-500 focus:ring-red-500 border p-2"
                                        placeholder="Radio CCBES En Vivo"
                                    />
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* General & About Config */}
                    <div className="bg-white rounded-lg shadow overflow-hidden">
                        <div className="px-6 py-4 border-b border-gray-200">
                            <h2 className="text-lg font-semibold text-gray-900">Sobre Nosotros y Contacto</h2>
                        </div>
                        <div className="p-6 space-y-4">
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-8 p-6 bg-gray-50/50 rounded-xl border border-gray-100">
                                <div className="space-y-4">
                                    <label className="block text-sm font-bold text-gray-700 uppercase tracking-wider">Subir Nueva Imagen</label>
                                    <ImageUploader
                                        currentImageUrl={aboutConfig.logoUrl}
                                        onImageUploaded={(url) => handleChange('about', { logoUrl: url })}
                                        aspectRatio="square"
                                        storagePath="images"
                                        label="Haz clic para actualizar logo"
                                    />
                                    <p className="text-xs text-gray-500 italic">
                                        * Recomendado: PNG con fondo transparente, 512x512px.
                                    </p>
                                </div>

                                <div className="flex flex-col">
                                    <label className="block text-sm font-bold text-gray-700 uppercase tracking-wider mb-4">Estado en Servidor</label>
                                    <div className="flex-1 min-h-[220px] bg-white rounded-lg border border-gray-200 shadow-sm overflow-hidden relative flex items-center justify-center p-6 group">
                                        {aboutConfig.logoUrl ? (
                                            <>
                                                <img
                                                    src={aboutConfig.logoUrl}
                                                    alt="Logo actual"
                                                    className="max-w-full max-h-full object-contain drop-shadow-sm transition-transform group-hover:scale-105"
                                                />
                                                <div className="absolute top-3 right-3 flex items-center gap-2 bg-green-50 px-2 py-1 rounded-full border border-green-100">
                                                    <span className="flex h-2 w-2 relative">
                                                        <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-green-400 opacity-75"></span>
                                                        <span className="relative inline-flex rounded-full h-2 w-2 bg-green-500"></span>
                                                    </span>
                                                    <span className="text-[10px] font-bold text-green-700 uppercase">En Línea</span>
                                                </div>
                                                <div className="absolute bottom-0 left-0 right-0 bg-black/70 text-white text-[9px] py-1.5 px-3 text-center opacity-0 group-hover:opacity-100 transition-all transform translate-y-1 group-hover:translate-y-0">
                                                    <span className="font-mono">{aboutConfig.logoUrl}</span>
                                                </div>
                                            </>
                                        ) : (
                                            <div className="text-center space-y-2">
                                                <div className="w-12 h-12 bg-gray-50 rounded-full flex items-center justify-center mx-auto border border-gray-100">
                                                    <svg className="w-6 h-6 text-gray-300" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                                                    </svg>
                                                </div>
                                                <span className="text-xs text-gray-400 font-semibold uppercase tracking-tight">Sin imagen cargada</span>
                                            </div>
                                        )}
                                    </div>
                                </div>
                            </div>

                            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700">Nombre de la Iglesia</label>
                                    <input
                                        type="text"
                                        value={aboutConfig.churchName}
                                        onChange={(e) => handleChange('about', { churchName: e.target.value })}
                                        className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-red-500 focus:ring-red-500 border p-2"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-700">Texto Secundario</label>
                                    <input
                                        type="text"
                                        value={aboutConfig.subText}
                                        onChange={(e) => handleChange('about', { subText: e.target.value })}
                                        className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-red-500 focus:ring-red-500 border p-2"
                                    />
                                </div>
                            </div>

                            <div>
                                <label className="block text-sm font-medium text-gray-700">Ubicación</label>
                                <input
                                    type="text"
                                    value={aboutConfig.location}
                                    onChange={(e) => handleChange('about', { location: e.target.value })}
                                    className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-red-500 focus:ring-red-500 border p-2"
                                />
                            </div>

                            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700">Email</label>
                                    <input
                                        type="email"
                                        value={aboutConfig.email}
                                        onChange={(e) => handleChange('about', { email: e.target.value })}
                                        className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-red-500 focus:ring-red-500 border p-2"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-700">Teléfono</label>
                                    <input
                                        type="text"
                                        value={aboutConfig.phone}
                                        onChange={(e) => handleChange('about', { phone: e.target.value })}
                                        className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-red-500 focus:ring-red-500 border p-2"
                                    />
                                </div>
                            </div>
                        </div>
                    </div>

                    {/* Social Media Config */}
                    <div className="bg-white rounded-lg shadow overflow-hidden">
                        <div className="px-6 py-4 border-b border-gray-200">
                            <h2 className="text-lg font-semibold text-gray-900">Redes Sociales</h2>
                        </div>
                        <div className="p-6 space-y-4">
                            <div>
                                <label className="block text-sm font-medium text-gray-700">Facebook URL</label>
                                <input
                                    type="url"
                                    value={aboutConfig.facebookUrl}
                                    onChange={(e) => handleChange('about', { facebookUrl: e.target.value })}
                                    className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-red-500 focus:ring-red-500 border p-2"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-700">Instagram URL</label>
                                <input
                                    type="url"
                                    value={aboutConfig.instagramUrl}
                                    onChange={(e) => handleChange('about', { instagramUrl: e.target.value })}
                                    className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-red-500 focus:ring-red-500 border p-2"
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-700">YouTube URL</label>
                                <input
                                    type="url"
                                    value={aboutConfig.youtubeUrl}
                                    onChange={(e) => handleChange('about', { youtubeUrl: e.target.value })}
                                    className="mt-1 block w-full rounded-md border-gray-300 shadow-sm focus:border-red-500 focus:ring-red-500 border p-2"
                                />
                            </div>
                        </div>
                    </div>

                    <div className="flex justify-end pt-4">
                        <button
                            type="submit"
                            disabled={saving || !hasChanges}
                            className="bg-red-600 text-white px-6 py-3 rounded-lg hover:bg-red-700 transition-colors disabled:opacity-50 font-bold shadow-md"
                        >
                            {saving ? 'Guardando...' : 'Guardar Todos los Cambios'}
                        </button>
                    </div>
                </form>
            </div>
        </div>
    );
}
