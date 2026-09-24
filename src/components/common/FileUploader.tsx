import React, { useRef, useState } from 'react';
import { UploadCloud, X, FileText, Image as ImageIcon, Video } from 'lucide-react';

interface FileUploaderProps {
  files: File[];
  onChange: (files: File[]) => void;
  maxFiles?: number;
  maxSizeMb?: number;
}

export const FileUploader: React.FC<FileUploaderProps> = ({
  files,
  onChange,
  maxFiles = 4,
  maxSizeMb = 10,
}) => {
  const [isDragOver, setIsDragOver] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  const handleFilesAdded = (incomingFiles: FileList | null) => {
    if (!incomingFiles) return;
    const newFileList = Array.from(incomingFiles);

    const validFiles = newFileList.filter((f) => {
      const isSizeOk = f.size <= maxSizeMb * 1024 * 1024;
      return isSizeOk;
    });

    const combined = [...files, ...validFiles].slice(0, maxFiles);
    onChange(combined);
  };

  const removeFile = (index: number) => {
    const updated = files.filter((_, i) => i !== index);
    onChange(updated);
  };

  const getFileIcon = (file: File) => {
    if (file.type.startsWith('image/')) return <ImageIcon className="w-5 h-5 text-blue-500" />;
    if (file.type.startsWith('video/')) return <Video className="w-5 h-5 text-purple-500" />;
    return <FileText className="w-5 h-5 text-slate-400" />;
  };

  return (
    <div className="w-full space-y-3">
      <div
        onDragOver={(e) => {
          e.preventDefault();
          setIsDragOver(true);
        }}
        onDragLeave={() => setIsDragOver(false)}
        onDrop={(e) => {
          e.preventDefault();
          setIsDragOver(false);
          handleFilesAdded(e.dataTransfer.files);
        }}
        onClick={() => inputRef.current?.click()}
        className={`border-2 border-dashed rounded-xl p-6 text-center cursor-pointer transition-colors ${
          isDragOver
            ? 'border-blue-500 bg-blue-50/50'
            : 'border-slate-300 hover:border-slate-400 bg-slate-50/50'
        }`}
      >
        <input
          ref={inputRef}
          type="file"
          multiple
          accept="image/*,video/*"
          className="hidden"
          onChange={(e) => handleFilesAdded(e.target.files)}
        />
        <div className="flex flex-col items-center justify-center space-y-2">
          <div className="w-10 h-10 rounded-full bg-blue-50 text-blue-600 flex items-center justify-center">
            <UploadCloud className="w-5 h-5" />
          </div>
          <div className="text-sm">
            <span className="font-semibold text-blue-600 hover:underline">Click to upload</span> or drag and drop
          </div>
          <p className="text-xs text-slate-500">
            Photos or short videos (Max {maxFiles} files, up to {maxSizeMb}MB each)
          </p>
        </div>
      </div>

      {files.length > 0 && (
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5">
          {files.map((file, idx) => (
            <div
              key={idx}
              className="flex items-center justify-between p-2.5 bg-white border border-slate-200 rounded-lg shadow-sm"
            >
              <div className="flex items-center space-x-2.5 truncate">
                {file.type.startsWith('image/') ? (
                  <img
                    src={URL.createObjectURL(file)}
                    alt={file.name}
                    className="w-9 h-9 object-cover rounded border border-slate-200"
                  />
                ) : (
                  getFileIcon(file)
                )}
                <div className="truncate">
                  <p className="text-xs font-medium text-slate-800 truncate">{file.name}</p>
                  <p className="text-[11px] text-slate-400">
                    {(file.size / (1024 * 1024)).toFixed(2)} MB
                  </p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => removeFile(idx)}
                className="p-1 text-slate-400 hover:text-red-600 rounded-md hover:bg-slate-100 transition-colors cursor-pointer"
              >
                <X className="w-4 h-4" />
              </button>
            </div>
          ))}
        </div>
      )}
    </div>
  );
};
