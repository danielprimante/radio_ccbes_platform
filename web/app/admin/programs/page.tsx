'use client';

import { useEffect, useState } from 'react';
import { useRouter } from 'next/navigation';
import {
    getPrograms,
    createProgram,
    updateProgram,
    deleteProgram,
    setActiveProgram,
    Program
} from '@/lib/api';
import { uploadImage, deleteImage } from '@/lib/storage';
import ImageUploader from '@/components/ImageUploader';

export default function ProgramsPage() {
    const [programs, setPrograms] = useState<Program[]>([]);
    const [loading, setLoading] = useState(true);
    const [saving, setSaving] = useState(false);
    const [message, setMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

    // Modal de creación/edición
    const [showModal, setShowModal] = useState(false);
    const [editingProgram, setEditingProgram] = useState<Program | null>(null);
    const [formName, setFormName] = useState('');
    const [formImageUrl, setFormImageUrl] = useState('');
    const [formImageDeleteUrl, setFormImageDeleteUrl] = useState('');

    useEffect(() => {
        loadPrograms();
    }, []);

    async function loadPrograms() {
        setLoading(true);
        try {
            const data = await getPrograms();
            setPrograms(data.sort((a, b) => a.name.localeCompare(b.name)));
        } catch (e) {
            showMessage('error', 'Error al cargar los programas');
        } finally {
            setLoading(false);
        }
    }

    function showMessage(type: 'success' | 'error', text: string) {
        setMessage({ type, text });
        setTimeout(() => setMessage(null), 4000);
    }

    function openCreateModal() {
        setEditingProgram(null);
        setFormName('');
        setFormImageUrl('');
        setFormImageDeleteUrl('');
        setShowModal(true);
    }

    function openEditModal(program: Program) {
        setEditingProgram(program);
        setFormName(program.name);
        setFormImageUrl(program.imageUrl || '');
        setFormImageDeleteUrl(program.imageDeleteUrl || '');
        setShowModal(true);
    }

    async function handleSaveProgram() {
        if (!formName.trim()) {
            showMessage('error', 'El nombre del programa es requerido.');
            return;
        }
        setSaving(true);
        try {
            if (editingProgram?.id) {
                // Si cambió la imagen, elimina la antigua
                if (editingProgram.imageDeleteUrl && editingProgram.imageUrl !== formImageUrl) {
                    await deleteImage(editingProgram.imageDeleteUrl).catch(() => { });
                }
                await updateProgram(editingProgram.id, {
                    name: formName.trim(),
                    imageUrl: formImageUrl,
                    imageDeleteUrl: formImageDeleteUrl || undefined
                });
                showMessage('success', `Programa "${formName}" actualizado.`);
            } else {
                await createProgram({
                    name: formName.trim(),
                    imageUrl: formImageUrl,
                    imageDeleteUrl: formImageDeleteUrl || undefined,
                    isActive: false
                });
                showMessage('success', `Programa "${formName}" creado.`);
            }
            setShowModal(false);
            await loadPrograms();
        } catch (e) {
            showMessage('error', 'Error al guardar el programa.');
        } finally {
            setSaving(false);
        }
    }

    async function handleDelete(program: Program) {
        if (!confirm(`¿Eliminar el programa "${program.name}"? Esta acción no se puede deshacer.`)) return;
        try {
            await deleteProgram(program.id!);
            showMessage('success', `Programa "${program.name}" eliminado.`);
            await loadPrograms();
        } catch (e) {
            showMessage('error', 'Error al eliminar el programa.');
        }
    }

    async function handleToggleActive(program: Program) {
        const willBeActive = !program.isActive;
        try {
            if (willBeActive) {
                // Activar este, desactivar el resto
                await setActiveProgram(program.id!);
                showMessage('success', `"${program.name}" está al aire ✅`);
            } else {
                // Desactivar todos
                await setActiveProgram(null);
                showMessage('success', 'Ningún programa al aire. Se muestra el logo original.');
            }
            await loadPrograms();
        } catch (e) {
            showMessage('error', 'Error al cambiar el estado del programa.');
        }
    }

    return (
        <div className="max-w-5xl mx-auto px-4 sm:px-6 lg:px-8 py-8">
            {/* Header */}
            <div className="flex items-center justify-between mb-8">
                <div>
                    <h1 className="text-2xl font-bold text-gray-900">Programas de Radio</h1>
                    <p className="text-gray-500 text-sm mt-1">
                        Gestiona los programas. Activa uno para que la app muestre su nombre e icono.
                    </p>
                </div>
                <button
                    onClick={openCreateModal}
                    className="bg-red-600 text-white px-4 py-2 rounded-lg hover:bg-red-700 font-semibold shadow transition-colors flex items-center gap-2"
                >
                    <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 4v16m8-8H4" />
                    </svg>
                    Nuevo Programa
                </button>
            </div>

            {/* Mensaje de estado */}
            {message && (
                <div className={`mb-6 p-4 rounded-lg border font-medium ${message.type === 'success' ? 'bg-green-50 text-green-700 border-green-200' : 'bg-red-50 text-red-700 border-red-200'}`}>
                    {message.text}
                </div>
            )}

            {loading ? (
                <div className="text-center py-20 text-gray-400 animate-pulse">Cargando programas...</div>
            ) : programs.length === 0 ? (
                <div className="text-center py-20 bg-gray-50 rounded-xl border-2 border-dashed border-gray-200">
                    <svg className="w-12 h-12 text-gray-300 mx-auto mb-3" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1.5} d="M19 11a7 7 0 01-7 7m0 0a7 7 0 01-7-7m7 7v4m0 0H8m4 0h4m-4-8a3 3 0 01-3-3V5a3 3 0 116 0v6a3 3 0 01-3 3z" />
                    </svg>
                    <p className="text-gray-500 font-medium">No hay programas creados aún</p>
                    <p className="text-gray-400 text-sm mt-1">Crea el primer programa de radio haciendo clic en "Nuevo Programa"</p>
                </div>
            ) : (
                <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-5">
                    {programs.map((program) => (
                        <div
                            key={program.id}
                            className={`bg-white rounded-xl shadow-sm border-2 transition-all overflow-hidden ${program.isActive ? 'border-red-400 shadow-red-100' : 'border-gray-100'}`}
                        >
                            {/* Imagen del programa */}
                            <div className="relative aspect-square bg-gray-50 flex items-center justify-center overflow-hidden">
                                {program.imageUrl ? (
                                    <img
                                        src={program.imageUrl}
                                        alt={program.name}
                                        className="w-full h-full object-contain p-4"
                                    />
                                ) : (
                                    <div className="flex flex-col items-center gap-2 text-gray-300">
                                        <svg className="w-16 h-16" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={1} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                                        </svg>
                                        <span className="text-xs font-medium">Sin icono</span>
                                    </div>
                                )}

                                {/* Badge "AL AIRE" */}
                                {program.isActive && (
                                    <div className="absolute top-3 left-3 bg-red-600 text-white text-xs font-bold px-2 py-1 rounded-full flex items-center gap-1.5">
                                        <span className="w-2 h-2 bg-white rounded-full animate-pulse inline-block"></span>
                                        AL AIRE
                                    </div>
                                )}
                            </div>

                            {/* Info y controles */}
                            <div className="p-4">
                                <h3 className="font-semibold text-gray-900 text-base truncate mb-3">{program.name}</h3>
                                <div className="flex items-center gap-2">
                                    {/* Toggle Al Aire */}
                                    <button
                                        onClick={() => handleToggleActive(program)}
                                        className={`flex-1 py-2 px-3 rounded-lg text-sm font-semibold transition-colors ${program.isActive
                                            ? 'bg-red-50 text-red-700 border border-red-200 hover:bg-red-100'
                                            : 'bg-gray-100 text-gray-600 hover:bg-gray-200'
                                            }`}
                                    >
                                        {program.isActive ? '⏹ Bajar' : '▶ Al Aire'}
                                    </button>

                                    {/* Editar */}
                                    <button
                                        onClick={() => openEditModal(program)}
                                        className="p-2 rounded-lg bg-gray-100 text-gray-600 hover:bg-blue-50 hover:text-blue-600 transition-colors"
                                        title="Editar"
                                    >
                                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M15.232 5.232l3.536 3.536m-2.036-5.036a2.5 2.5 0 113.536 3.536L6.5 21.036H3v-3.572L16.732 3.732z" />
                                        </svg>
                                    </button>

                                    {/* Eliminar */}
                                    <button
                                        onClick={() => handleDelete(program)}
                                        className="p-2 rounded-lg bg-gray-100 text-gray-600 hover:bg-red-50 hover:text-red-600 transition-colors"
                                        title="Eliminar"
                                    >
                                        <svg className="w-4 h-4" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 7l-.867 12.142A2 2 0 0116.138 21H7.862a2 2 0 01-1.995-1.858L5 7m5 4v6m4-6v6m1-10V4a1 1 0 00-1-1h-4a1 1 0 00-1 1v3M4 7h16" />
                                        </svg>
                                    </button>
                                </div>
                            </div>
                        </div>
                    ))}
                </div>
            )}

            {/* ─── Modal Crear / Editar ─── */}
            {showModal && (
                <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40">
                    <div className="bg-white rounded-2xl shadow-2xl w-full max-w-md mx-4 overflow-hidden">
                        <div className="px-6 py-4 border-b border-gray-100 flex items-center justify-between">
                            <h2 className="text-lg font-bold text-gray-900">
                                {editingProgram ? 'Editar Programa' : 'Nuevo Programa'}
                            </h2>
                            <button
                                onClick={() => setShowModal(false)}
                                className="text-gray-400 hover:text-gray-600 transition-colors"
                            >
                                <svg className="w-5 h-5" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M6 18L18 6M6 6l12 12" />
                                </svg>
                            </button>
                        </div>

                        <div className="p-6 space-y-5">
                            {/* Nombre */}
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-1">Nombre del Programa *</label>
                                <input
                                    type="text"
                                    value={formName}
                                    onChange={(e) => setFormName(e.target.value)}
                                    placeholder="Ej: Alabanza de la Mañana"
                                    className="w-full border border-gray-300 rounded-lg px-3 py-2 focus:outline-none focus:ring-2 focus:ring-red-400 text-sm"
                                />
                            </div>

                            {/* Icono del programa */}
                            <div>
                                <label className="block text-sm font-medium text-gray-700 mb-2">Icono del Programa</label>
                                <ImageUploader
                                    currentImageUrl={formImageUrl}
                                    onImageUploaded={(url, deleteUrl) => {
                                        setFormImageUrl(url);
                                        setFormImageDeleteUrl(deleteUrl || '');
                                    }}
                                    aspectRatio="square"
                                    storagePath="programs"
                                    label="Subir icono (PNG recomendado, 512×512)"
                                />
                                <p className="text-xs text-gray-400 mt-1 italic">Si no se sube icono, se usará el logo principal de la radio.</p>
                            </div>
                        </div>

                        <div className="px-6 pb-6 flex justify-end gap-3">
                            <button
                                onClick={() => setShowModal(false)}
                                className="px-4 py-2 text-sm font-medium text-gray-600 bg-gray-100 rounded-lg hover:bg-gray-200 transition-colors"
                            >
                                Cancelar
                            </button>
                            <button
                                onClick={handleSaveProgram}
                                disabled={saving}
                                className="px-5 py-2 text-sm font-bold text-white bg-red-600 rounded-lg hover:bg-red-700 transition-colors disabled:opacity-50"
                            >
                                {saving ? 'Guardando...' : editingProgram ? 'Guardar Cambios' : 'Crear Programa'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
