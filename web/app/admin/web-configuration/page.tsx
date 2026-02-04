"use client";

import { useState, useEffect } from "react";
import { doc, getDoc, setDoc, collection, getDocs, deleteDoc, query, orderBy } from "firebase/firestore";
import { db } from "@/lib/firebase";
import ImageUploader from "@/components/ImageUploader";

interface LandingData {
    hero: {
        title: string;
        subtitle: string;
        year: string; // Used as "Encabezado"
        backgroundImage: string;
        whatsapp: string;
    };
    events: string[];
    offering: {
        message: string;
        alias: string;
        cbu: string;
    };
    radio: {
        qrImage: string;
        playStoreLink: string;
        privacyPolicyLink: string;
    };
}

interface PrayerRequest {
    id: string;
    name: string;
    phone: string;
    request: string;
    timestamp: any;
}

export default function WebConfiguration() {
    const [activeTab, setActiveTab] = useState<'config' | 'prayer'>('config');
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);

    // Config State
    const [data, setData] = useState<LandingData>({
        hero: {
            title: "Conferencia\nCCBES Jóvenes",
            subtitle: "16 y 17 de Feb",
            year: "2026",
            backgroundImage: "https://lh3.googleusercontent.com/aida-public/AB6AXuCa-bv6foFA7y7iojRzQggAtyTJ5JSApJ_MelbVwAsMIbpOfcK2nQT8vjc_-ha6j09ez7ZJWEFdaMMdhxr7HirEkjdWbAj9BRhBzLSClyNA7VCX1E2fUetuer78B9oAV8j1906UYk0snCHH4qZpN0Dw84mK3IGNgQGIpSws0RP0ipOCxKz27ZI6QmmXm82ufbgKhdXZGsNwGd1tFZKGEMvZC1BzelWHrgTDE7jBMxdD3qdxml20VXNWhMEC447lnOYzV_Z_IRGjwyk",
            whatsapp: "+54.11.4789.0047"
        },
        events: [
            "https://lh3.googleusercontent.com/aida-public/AB6AXuDB6dq0OGHdrJGh5qKACNwNVz9-p7qxaUsMMNlN3YO2xvR4pruj86l8OaXfP4xPVFgN9_GMKOSa61Qpkiev6v9wc4RxESKY_su4tXyxdogwLYwBsE_K2qhy0fI7AUNaF2LmCo6Bifh2YTBvqZQZM-Sapytxlz_yDbvcuqIgLA0ZOT5Ku2Q3oVUf_aRKRBp8eC_pfFzt_O0FwplOTYzZUaqYfwDZCwRas6Ao0q3Um3fmXstsss5r5y9s-4wcnawrtU9UEVdvzO1wJdA",
            "https://lh3.googleusercontent.com/aida-public/AB6AXuD7HVzI5QJMU4-AEJA_1ihJBRIz_sg7jBHMoxGGubssBjndM_lBBbqYQBRO-7Q5zBPunXZFYTcL50mHzR6O_GvrkLueSzf9UYedrO9dlxb0XoxLShrva-wI8S5F7v3xBDJzSSgVWJbmeRJV4eB0nv6HrGeM14oWIylsT5Mj9zHRDzviPSsr-LLrxxFcgfiVrkPM49NDm00gsLCd9Y8cW47EqVG4QEC9uv8jwq-axkFC9U3zeRftWYmlh7kYpBo5E6JwmhROAJeS2Bg",
            "https://lh3.googleusercontent.com/aida-public/AB6AXuDBuOYQligVtB_vg1-CHN5jGWYrE0uMx5Ph8rgZbjnJ0x8AW_xfq1rF8gvGMNewN_1G4EPIPWo4goN-UxnHAr0s0XjC3UPnrgnmX4mteuRz3R4hxXqIphYCbut6bJkXPFu-FnZEvs4O55Nbpoa32KL-oXKr4_cXYYEwbs4fenqFLioo2c0DsWkFlBg11eIu3pu2t4m3CVA2jR4ZLhg2HqHhsHNE3WCxO95DIeS9fHXFHxBFRN3OPXOStfU_WJr2iDqrz6sJYAd_qmQ"
        ],
        offering: {
            message: "Gracias por colaborar para que la vision de Dios siga creciendo",
            alias: "",
            cbu: ""
        },
        radio: {
            qrImage: "https://lh3.googleusercontent.com/aida-public/AB6AXuAYWtL7z9oEpxB7hg6XzrdVqcMh6ypceshnp-oeYWrZidjFQ5znpy08zGLAqMncMhZWnP0qQQJ5DDhLoogbr01QjhYonS0UoPpfUkYOhPRPzn8AkU6g3-3hHjXG6FlJL1pT-67RSyEk1YjC-LpPuTuVTMkO5WiCLh7CqqAZMJTcfAuRNG6AzySTa12Pk3fC0Av6aZ9hEzoqezFpWabVvNmYC7uHS-5NKS_sHoRKwh7moHM59-NiBEWmzXdGqebBuavjK0KDj0bdNmQ",
            playStoreLink: "https://play.google.com/store/apps/details?id=com.radiocristiana",
            privacyPolicyLink: "/politicas"
        }
    });

    // Prayer Requests State
    const [requests, setRequests] = useState<PrayerRequest[]>([]);
    const [loadingRequests, setLoadingRequests] = useState(false);

    useEffect(() => {
        fetchConfigData();
    }, []);

    useEffect(() => {
        if (activeTab === 'prayer') {
            fetchPrayerRequests();
        }
    }, [activeTab]);

    const fetchConfigData = async () => {
        try {
            const docRef = doc(db, "content", "landing");
            const docSnap = await getDoc(docRef);
            if (docSnap.exists()) {
                const fetchedData = docSnap.data() as LandingData;
                setData(prev => ({
                    ...prev,
                    ...fetchedData,
                    hero: { ...prev.hero, ...(fetchedData.hero || {}) },
                    offering: { ...prev.offering, ...(fetchedData.offering || {}) },
                    radio: { ...prev.radio, ...(fetchedData.radio || {}) }
                }));
            } else {
                await setDoc(docRef, data);
            }
        } catch (error) {
            console.error("Error fetching landing data:", error);
        } finally {
            setLoading(false);
        }
    };

    const fetchPrayerRequests = async () => {
        setLoadingRequests(true);
        try {
            const q = query(collection(db, "prayer_requests"), orderBy("timestamp", "desc"));
            const querySnapshot = await getDocs(q);
            const reqs: PrayerRequest[] = [];
            querySnapshot.forEach((doc) => {
                reqs.push({ id: doc.id, ...doc.data() } as PrayerRequest);
            });
            setRequests(reqs);
        } catch (error) {
            console.error("Error fetching prayer requests:", error);
        } finally {
            setLoadingRequests(false);
        }
    };

    const handleDeleteRequest = async (id: string) => {
        if (!confirm("¿Está seguro de eliminar este pedido?")) return;
        try {
            await deleteDoc(doc(db, "prayer_requests", id));
            setRequests(prev => prev.filter(r => r.id !== id));
        } catch (error) {
            console.error("Error deleting request:", error);
            alert("Error al eliminar");
        }
    };

    const handleHeroChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
        const { name, value } = e.target;
        setData(prev => ({
            ...prev,
            hero: { ...prev.hero, [name]: value }
        }));
    };

    const handleOfferingChange = (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) => {
        const { name, value } = e.target;
        setData(prev => ({
            ...prev,
            offering: { ...prev.offering, [name]: value }
        }));
    };

    const handleHeroImageUpload = (url: string) => {
        setData(prev => ({
            ...prev,
            hero: { ...prev.hero, backgroundImage: url }
        }));
    };

    const handleQrImageUpload = (url: string) => {
        setData(prev => ({
            ...prev,
            radio: { ...prev.radio, qrImage: url }
        }));
    };

    const handlePlayStoreLinkChange = (e: React.ChangeEvent<HTMLInputElement>) => {
        const { name, value } = e.target;
        setData(prev => ({
            ...prev,
            radio: { ...prev.radio, [name]: value }
        }));
    };

    const handleEventImageUpload = async (url: string) => {
        const updatedEvents = [...data.events, url];
        setData(prev => ({ ...prev, events: updatedEvents }));
    };

    const removeEventImage = (indexToRemove: number) => {
        setData(prev => ({
            ...prev,
            events: prev.events.filter((_, index) => index !== indexToRemove)
        }));
    };

    const saveChanges = async () => {
        setSaving(true);
        try {
            const docRef = doc(db, "content", "landing");
            await setDoc(docRef, data);
            alert("Cambios guardados correctamente");
        } catch (error) {
            console.error("Error saving changes:", error);
            alert("Error al guardar los cambios");
        } finally {
            setSaving(false);
        }
    };

    if (loading) return <div className="p-8">Cargando configuración...</div>;

    return (
        <div className="max-w-6xl mx-auto p-6 md:p-8 space-y-8">
            <div className="flex justify-between items-center">
                <h1 className="text-2xl font-bold text-gray-900">Landing Page y Pedidos</h1>
                {activeTab === 'config' && (
                    <button
                        onClick={saveChanges}
                        disabled={saving}
                        className="bg-primary text-white px-6 py-2 rounded-lg font-semibold hover:opacity-90 transition disabled:opacity-50"
                    >
                        {saving ? "Guardando..." : "Guardar Cambios"}
                    </button>
                )}
            </div>

            {/* Tabs */}
            <div className="flex border-b border-gray-200">
                <button
                    onClick={() => setActiveTab('config')}
                    className={`px-6 py-3 font-medium text-sm transition-colors relative ${activeTab === 'config'
                        ? "text-primary border-b-2 border-primary"
                        : "text-gray-500 hover:text-gray-700"
                        }`}
                >
                    Configuración Web
                </button>
                <button
                    onClick={() => setActiveTab('prayer')}
                    className={`px-6 py-3 font-medium text-sm transition-colors relative ${activeTab === 'prayer'
                        ? "text-primary border-b-2 border-primary"
                        : "text-gray-500 hover:text-gray-700"
                        }`}
                >
                    Pedidos de Oración
                </button>
            </div>

            {/* Content */}
            {activeTab === 'config' ? (
                <div className="space-y-8 animate-in fade-in duration-300">
                    {/* Hero Configuration */}
                    <section className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 space-y-6">
                        <h2 className="text-xl font-semibold border-b pb-4">Sección Hero</h2>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                            <div className="space-y-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">Encabezado (Superior)</label>
                                    <input
                                        type="text"
                                        name="year"
                                        value={data.hero.year}
                                        onChange={handleHeroChange}
                                        className="w-full rounded-md border-gray-300 shadow-sm focus:border-primary focus:ring-primary"
                                        placeholder="Ej: 2026"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">Título Principal</label>
                                    <textarea
                                        name="title"
                                        value={data.hero.title}
                                        onChange={handleHeroChange}
                                        rows={2}
                                        className="w-full rounded-md border-gray-300 shadow-sm focus:border-primary focus:ring-primary"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">Subtítulo</label>
                                    <input
                                        type="text"
                                        name="subtitle"
                                        value={data.hero.subtitle}
                                        onChange={handleHeroChange}
                                        className="w-full rounded-md border-gray-300 shadow-sm focus:border-primary focus:ring-primary"
                                    />
                                </div>
                                <hr className="my-2 border-gray-100" />
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">WhatsApp / Teléfono</label>
                                    <input
                                        type="text"
                                        name="whatsapp"
                                        value={data.hero.whatsapp || ""}
                                        onChange={handleHeroChange}
                                        className="w-full rounded-md border-gray-300 shadow-sm focus:border-primary focus:ring-primary"
                                        placeholder="+54..."
                                    />
                                </div>
                            </div>
                            <div className="space-y-2">
                                <label className="block text-sm font-medium text-gray-700">Imagen de Fondo</label>
                                <div className="aspect-video relative rounded-lg overflow-hidden bg-gray-100 border border-gray-200">
                                    {data.hero.backgroundImage && (
                                        <img
                                            src={data.hero.backgroundImage}
                                            alt="Hero Background"
                                            className="w-full h-full object-cover"
                                        />
                                    )}
                                </div>
                                <div className="mt-2">
                                    <ImageUploader onImageUploaded={handleHeroImageUpload} storagePath="hero" />
                                </div>
                            </div>
                        </div>
                    </section>

                    {/* Offering Configuration */}
                    <section className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 space-y-6">
                        <h2 className="text-xl font-semibold border-b pb-4">Configuración de Ofrendas</h2>
                        <div className="grid grid-cols-1 gap-4">
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Mensaje</label>
                                <input
                                    type="text"
                                    name="message"
                                    value={data.offering.message}
                                    onChange={handleOfferingChange}
                                    maxLength={100}
                                    className="w-full rounded-md border-gray-300 shadow-sm focus:border-primary focus:ring-primary"
                                />
                            </div>
                            <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">Alias (max 20 chars)</label>
                                    <input
                                        type="text"
                                        name="alias"
                                        value={data.offering.alias}
                                        onChange={handleOfferingChange}
                                        maxLength={20}
                                        className="w-full rounded-md border-gray-300 shadow-sm focus:border-primary focus:ring-primary"
                                    />
                                </div>
                                <div>
                                    <label className="block text-sm font-medium text-gray-700 mb-1">CBU / CVU (22 digits)</label>
                                    <input
                                        type="text"
                                        name="cbu"
                                        value={data.offering.cbu}
                                        onChange={handleOfferingChange}
                                        maxLength={22}
                                        className="w-full rounded-md border-gray-300 shadow-sm focus:border-primary focus:ring-primary"
                                    />
                                </div>
                            </div>
                        </div>
                    </section>

                    {/* Radio Configuration */}
                    <section className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 space-y-6">
                        <h2 className="text-xl font-semibold border-b pb-4">Configuración de Radio</h2>
                        <div className="space-y-2">
                            <label className="block text-sm font-medium text-gray-700">Código QR App</label>
                            <div className="flex gap-6 items-start">
                                <div className="w-48 aspect-square relative rounded-lg overflow-hidden bg-gray-100 border border-gray-200 flex-shrink-0">
                                    {data.radio?.qrImage && (
                                        <img
                                            src={data.radio.qrImage}
                                            alt="QR Code"
                                            className="w-full h-full object-contain p-2"
                                        />
                                    )}
                                </div>
                                <div className="flex-1">
                                    <p className="text-sm text-gray-500 mb-2">Sube la imagen del código QR para la sección de Radio.</p>
                                    <ImageUploader
                                        onImageUploaded={handleQrImageUpload}
                                        storagePath="radio"
                                        currentImageUrl={data.radio?.qrImage}
                                        aspectRatio="square"
                                    />
                                </div>
                            </div>
                        </div>
                        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Link de Google Play</label>
                                <input
                                    type="text"
                                    name="playStoreLink"
                                    value={data.radio?.playStoreLink || ""}
                                    onChange={handlePlayStoreLinkChange}
                                    className="w-full rounded-md border-gray-300 shadow-sm focus:border-primary focus:ring-primary"
                                    placeholder="https://play.google.com/store/apps/details?id=..."
                                />
                            </div>
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Link de Políticas de Privacidad</label>
                                <input
                                    type="text"
                                    name="privacyPolicyLink"
                                    value={data.radio?.privacyPolicyLink || ""}
                                    onChange={handlePlayStoreLinkChange}
                                    className="w-full rounded-md border-gray-300 shadow-sm focus:border-primary focus:ring-primary"
                                    placeholder="https://ejemplo.com/politicas"
                                />
                            </div>
                        </div>
                    </section>

                    {/* Events Configuration */}
                    <section className="bg-white p-6 rounded-xl shadow-sm border border-gray-100 space-y-6">
                        <h2 className="text-xl font-semibold border-b pb-4">Imágenes de Eventos</h2>
                        <div className="grid grid-cols-2 md:grid-cols-3 gap-4">
                            {data.events.map((url, index) => (
                                <div key={index} className="relative group aspect-square rounded-lg overflow-hidden border border-gray-200">
                                    <img src={url} alt={`Evento ${index + 1}`} className="w-full h-full object-cover" />
                                    <button
                                        onClick={() => removeEventImage(index)}
                                        className="absolute top-2 right-2 bg-red-600 text-white p-1 rounded-full opacity-0 group-hover:opacity-100 transition shadow-sm"
                                        title="Eliminar imagen"
                                    >
                                        <span className="material-symbols-outlined text-sm">close</span>
                                    </button>
                                </div>
                            ))}
                            <div className="aspect-square rounded-lg border-2 border-dashed border-gray-300 flex flex-col items-center justify-center p-4 hover:bg-gray-50 transition">
                                <span className="material-symbols-outlined text-gray-400 text-3xl mb-2">add_photo_alternate</span>
                                <span className="text-xs text-gray-500 text-center mb-2">Agregar Nueva</span>
                                <div className="w-full">
                                    <ImageUploader onImageUploaded={handleEventImageUpload} storagePath="events" mini={true} />
                                </div>
                            </div>
                        </div>
                    </section>
                </div>
            ) : (
                <div className="animate-in fade-in duration-300">
                    <div className="bg-white rounded-xl shadow-sm border border-gray-100 overflow-hidden">
                        <div className="p-6 border-b border-gray-100">
                            <h2 className="text-xl font-semibold">Pedidos Recibidos</h2>
                        </div>
                        {loadingRequests ? (
                            <div className="p-8 text-center text-gray-500">Cargando pedidos...</div>
                        ) : requests.length === 0 ? (
                            <div className="p-12 text-center text-gray-500">No hay pedidos de oración pendientes.</div>
                        ) : (
                            <div className="overflow-x-auto">
                                <table className="w-full text-left">
                                    <thead className="bg-gray-50 text-gray-600 text-sm uppercase">
                                        <tr>
                                            <th className="px-6 py-4">Fecha</th>
                                            <th className="px-6 py-4">Nombre</th>
                                            <th className="px-6 py-4">Teléfono</th>
                                            <th className="px-6 py-4">Pedido</th>
                                            <th className="px-6 py-4 text-right">Acciones</th>
                                        </tr>
                                    </thead>
                                    <tbody className="divide-y divide-gray-100">
                                        {requests.map((req) => (
                                            <tr key={req.id} className="hover:bg-gray-50 transition">
                                                <td className="px-6 py-4 text-sm text-gray-500 whitespace-nowrap">
                                                    {req.timestamp?.seconds ? new Date(req.timestamp.seconds * 1000).toLocaleDateString() : '-'}
                                                </td>
                                                <td className="px-6 py-4 font-medium text-gray-900">{req.name}</td>
                                                <td className="px-6 py-4 text-gray-600">{req.phone}</td>
                                                <td className="px-6 py-4 text-gray-600 max-w-xs truncate" title={req.request}>
                                                    {req.request}
                                                </td>
                                                <td className="px-6 py-4 text-right">
                                                    <button
                                                        onClick={() => handleDeleteRequest(req.id)}
                                                        className="text-red-500 hover:text-red-700 font-medium text-sm border border-red-200 hover:bg-red-50 px-3 py-1 rounded transition"
                                                    >
                                                        Eliminar
                                                    </button>
                                                </td>
                                            </tr>
                                        ))}
                                    </tbody>
                                </table>
                            </div>
                        )}
                    </div>
                </div>
            )}
        </div>
    );
}
