import React from 'react';
import ReactDOM from 'react-dom';

interface DropdownMenuProps {
  children: React.ReactNode;
}

interface DropdownMenuTriggerProps {
  children: React.ReactNode;
  asChild?: boolean;
}

interface DropdownMenuContentProps {
  children: React.ReactNode;
  className?: string;
  align?: 'start' | 'center' | 'end';
}

interface DropdownMenuItemProps {
  children: React.ReactNode;
  onClick?: () => void;
  className?: string;
}

interface DropdownContextValue {
  isOpen: boolean;
  setIsOpen: (open: boolean) => void;
  triggerRef: React.RefObject<HTMLDivElement>;
}

const DropdownContext = React.createContext<DropdownContextValue>({
  isOpen: false,
  setIsOpen: () => {},
  triggerRef: { current: null },
});

export const DropdownMenu: React.FC<DropdownMenuProps> = ({ children }) => {
  const [isOpen, setIsOpen] = React.useState(false);
  const triggerRef = React.useRef<HTMLDivElement>(null);

  return (
    <DropdownContext.Provider value={{ isOpen, setIsOpen, triggerRef }}>
      <div className="relative inline-block text-left">{children}</div>
    </DropdownContext.Provider>
  );
};

export const DropdownMenuTrigger: React.FC<DropdownMenuTriggerProps> = ({ children }) => {
  const { isOpen, setIsOpen, triggerRef } = React.useContext(DropdownContext);

  const handleClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    setIsOpen(!isOpen);
  };

  return (
    <div ref={triggerRef} onClick={handleClick}>
      {children}
    </div>
  );
};

export const DropdownMenuContent: React.FC<DropdownMenuContentProps> = ({
  children,
  className = '',
  align = 'end',
}) => {
  const { isOpen, setIsOpen, triggerRef } = React.useContext(DropdownContext);
  const contentRef = React.useRef<HTMLDivElement>(null);
  const [position, setPosition] = React.useState({ top: 0, left: 0 });
  const [openDirection, setOpenDirection] = React.useState<'down' | 'up'>('down');

  // Calculate position when opening
  React.useEffect(() => {
    if (isOpen && triggerRef.current) {
      const triggerRect = triggerRef.current.getBoundingClientRect();
      const contentHeight = 300; // Estimated max height
      const viewportHeight = window.innerHeight;
      const spaceBelow = viewportHeight - triggerRect.bottom;
      const spaceAbove = triggerRect.top;

      // Decide direction: open up if not enough space below and more space above
      const shouldOpenUp = spaceBelow < contentHeight && spaceAbove > spaceBelow;
      setOpenDirection(shouldOpenUp ? 'up' : 'down');

      let top: number;
      if (shouldOpenUp) {
        top = triggerRect.top + window.scrollY - 8; // Will be positioned from bottom
      } else {
        top = triggerRect.bottom + window.scrollY + 8;
      }

      let left: number;
      const menuWidth = 224; // w-56 = 14rem = 224px

      if (align === 'end') {
        left = triggerRect.right - menuWidth;
      } else if (align === 'start') {
        left = triggerRect.left;
      } else {
        left = triggerRect.left + triggerRect.width / 2 - menuWidth / 2;
      }

      // Keep within viewport horizontally
      left = Math.max(8, Math.min(left, window.innerWidth - menuWidth - 8));

      setPosition({ top, left });
    }
  }, [isOpen, align, triggerRef]);

  // Close on click outside
  React.useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (
        contentRef.current &&
        !contentRef.current.contains(event.target as Node) &&
        triggerRef.current &&
        !triggerRef.current.contains(event.target as Node)
      ) {
        setIsOpen(false);
      }
    };

    const handleEscape = (event: KeyboardEvent) => {
      if (event.key === 'Escape') {
        setIsOpen(false);
      }
    };

    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
      document.addEventListener('keydown', handleEscape);
    }

    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
      document.removeEventListener('keydown', handleEscape);
    };
  }, [isOpen, setIsOpen, triggerRef]);

  if (!isOpen) return null;

  const content = (
    <div
      ref={contentRef}
      style={{
        position: 'absolute',
        top: openDirection === 'down' ? position.top : 'auto',
        bottom: openDirection === 'up' ? `calc(100vh - ${position.top}px)` : 'auto',
        left: position.left,
        zIndex: 9999,
      }}
      className={`w-56 rounded-md border border-gray-200 dark:border-gray-700 bg-white dark:bg-gray-800 shadow-lg ${className}`}
    >
      <div className="py-1 max-h-80 overflow-y-auto">{children}</div>
    </div>
  );

  // Render using Portal to escape any overflow constraints
  return ReactDOM.createPortal(content, document.body);
};

export const DropdownMenuItem: React.FC<DropdownMenuItemProps> = ({
  children,
  onClick,
  className = '',
}) => {
  const { setIsOpen } = React.useContext(DropdownContext);

  const handleClick = (e: React.MouseEvent) => {
    e.stopPropagation();
    onClick?.();
    setIsOpen(false);
  };

  return (
    <button
      onClick={handleClick}
      className={`w-full text-left px-4 py-2 text-sm text-gray-700 dark:text-gray-300 hover:bg-gray-100 dark:hover:bg-gray-700 flex items-center ${className}`}
    >
      {children}
    </button>
  );
};

export const DropdownMenuLabel: React.FC<{ children: React.ReactNode; className?: string }> = ({
  children,
  className = '',
}) => {
  return (
    <div className={`px-4 py-2 text-sm font-semibold text-gray-900 dark:text-white ${className}`}>
      {children}
    </div>
  );
};

export const DropdownMenuSeparator: React.FC = () => {
  return <div className="my-1 h-px bg-gray-200 dark:bg-gray-700" />;
};
