'use client';

import { useState, useRef, useId, useEffect } from 'react';
import { uploadImage, compressImage } from '@/lib/storage';

interface ImageUploaderProps {
    onImageUploaded: (url: string, deleteUrl?: string) => void;
    currentImageUrl?: string;
    className?: string;
    aspectRatio?: 'square' | 'video' | 'any';
    label?: string;
    storagePath?: string;
    mini?: boolean;
}

export default function ImageUploader({
    onImageUploaded,
    currentImageUrl,
    className = '',
    aspectRatio = 'video',
    label = 'Haz clic para subir una imagen',
    storagePath = 'posts',
    mini = false
}: ImageUploaderProps) {
    const [uploading, setUploading] = useState(false);
    const [preview, setPreview] = useState<string | null>(currentImageUrl || null);
    const [error, setError] = useState<string>('');
    const fileInputRef = useRef<HTMLInputElement>(null);
    const uniqueId = useId();

    useEffect(() => {
        setPreview(currentImageUrl || null);
    }, [currentImageUrl]);

    const handleFileSelect = async (e: React.ChangeEvent<HTMLInputElement>) => {
        const file = e.target.files?.[0];
        if (!file) return;

        console.log('Iniciando subida de archivo:', file.name, file.size);

        // Validate file type
        if (!file.type.startsWith('image/')) {
            setError('Por favor selecciona una imagen válida');
            return;
        }

        // Validate file size (max 5MB)
        if (file.size > 5 * 1024 * 1024) {
            setError('La imagen no debe superar los 5MB');
            return;
        }

        setError('');
        setUploading(true);

        let objectUrl: string | null = null;
        try {
            // Show preview immediately using local URL
            objectUrl = URL.createObjectURL(file);
            setPreview(objectUrl);

            console.log('Comprimiendo imagen...');
            const compressedFile = await compressImage(file);
            console.log('Imagen comprimida:', compressedFile.size);



            // ...

            console.log('Subiendo a Firebase Storage en path:', storagePath);
            const data = await uploadImage(compressedFile, storagePath);
            console.log('Subida completada. URL:', data.url);

            onImageUploaded(data.url, data.delete_url);

        } catch (err: any) {
            console.error('Error en el proceso de subida:', err);
            setError(err.message || 'Error al subir la imagen');
            setPreview(currentImageUrl || null);
        } finally {
            if (objectUrl) URL.revokeObjectURL(objectUrl);
            setUploading(false);
            console.log('Estado de subida finalizado');
        }
    };

    const handleRemove = () => {
        setPreview(null);
        onImageUploaded('');
        if (fileInputRef.current) {
            fileInputRef.current.value = '';
        }
    };

    const aspectRatioClasses = {
        square: 'aspect-square max-h-48',
        video: 'aspect-video h-64',
        any: 'min-h-48'
    };

    return (
        <div className={className}>
            <input
                ref={fileInputRef}
                type="file"
                accept="image/*"
                onChange={handleFileSelect}
                className="hidden"
                id={uniqueId}
            />

            {preview ? (
                <div className={`relative group overflow-hidden rounded-lg border border-gray-200 bg-gray-50 ${aspectRatioClasses[aspectRatio]}`}>
                    <img
                        src={preview}
                        alt="Preview"
                        className={`w-full h-full ${aspectRatio === 'square' ? 'object-contain p-4' : 'object-cover'}`}
                    />

                    <div className="absolute inset-0 bg-black/40 opacity-0 group-hover:opacity-100 transition-opacity flex items-center justify-center gap-3">
                        <button
                            type="button"
                            onClick={handleRemove}
                            className="bg-white/90 text-red-600 px-4 py-2 rounded-full text-sm font-semibold hover:bg-white transition-colors shadow-lg"
                        >
                            Eliminar
                        </button>
                        <label
                            htmlFor={uniqueId}
                            className="bg-red-600 text-white px-4 py-2 rounded-full text-sm font-semibold hover:bg-red-700 transition-colors cursor-pointer shadow-lg"
                        >
                            Cambiar
                        </label>
                    </div>

                    {uploading && (
                        <div className="absolute inset-0 bg-black/60 flex flex-col items-center justify-center">
                            <div className="w-8 h-8 border-4 border-red-500 border-t-transparent rounded-full animate-spin mb-2"></div>
                            <div className="text-white text-xs font-medium">Subiendo...</div>
                        </div>
                    )}
                </div>
            ) : (
                <label
                    htmlFor={uniqueId}
                    className={`block w-full border-2 border-dashed border-gray-300 rounded-lg hover:border-red-500 hover:bg-red-50/10 transition-all cursor-pointer ${aspectRatioClasses[aspectRatio]}`}
                >
                    <div className={`h-full flex flex-col items-center justify-center text-gray-500 ${mini ? 'p-2' : 'p-6'}`}>
                        {uploading ? (
                            <div className="flex flex-col items-center">
                                <div className={`border-4 border-red-500 border-t-transparent rounded-full animate-spin mb-2 ${mini ? 'w-6 h-6' : 'w-8 h-8'}`}></div>
                                {!mini && <span className="text-sm font-medium">Subiendo...</span>}
                            </div>
                        ) : (
                            <>
                                <div className={`${mini ? 'w-8 h-8 mb-1' : 'w-12 h-12 mb-4'} bg-gray-100 rounded-full flex items-center justify-center group-hover:bg-red-100`}>
                                    <svg className={`${mini ? 'w-4 h-4' : 'w-6 h-6'} text-gray-400 group-hover:text-red-500`} fill="none" stroke="currentColor" viewBox="0 0 24 24">
                                        <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 16l4.586-4.586a2 2 0 012.828 0L16 16m-2-2l1.586-1.586a2 2 0 012.828 0L20 14m-6-6h.01M6 20h12a2 2 0 002-2V6a2 2 0 00-2-2H6a2 2 0 00-2 2v12a2 2 0 002 2z" />
                                    </svg>
                                </div>
                                {!mini && (
                                    <>
                                        <span className="text-sm font-medium text-center">{label}</span>
                                        <span className="text-xs mt-1 text-gray-400">PNG, JPG hasta 5MB</span>
                                    </>
                                )}
                            </>
                        )}
                    </div>
                </label>
            )}

            {error && (
                <div className="mt-2 text-sm text-red-600 flex items-center gap-1">
                    <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 20 20">
                        <path fillRule="evenodd" d="M18 10a8 8 0 11-16 0 8 8 0 0116 0zm-7 4a1 1 0 11-2 0 1 1 0 012 0zm-1-9a1 1 0 00-1 1v4a1 1 0 102 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                    </svg>
                    {error}
                </div>
            )}
        </div>
    );
}
